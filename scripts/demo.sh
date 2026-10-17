#!/usr/bin/env bash
# End-to-end demo through the API gateway. Requires: curl, jq.
set -uo pipefail
GW=${GW:-http://localhost:8080}
RUN=$(date +%s)
hdr=(-H "Content-Type: application/json")

section() { printf '\n\033[1;36m=== %s ===\033[0m\n' "$1"; }

section "1. Eureka registry (expect 2x SPOT-ALLOCATION-SERVICE)"
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps \
  | jq -r '.applications.application[] | "\(.name): \(.instance | length) instance(s)"'

section "2. Availability (public, via gateway)"
curl -s "$GW/api/v1/spots/available?vehicleType=CAR" | jq '{servedBy, totalAvailable}'

section "3. Vehicle entry: gateway -> ticket-service -> (LB) -> spot-allocation"
ENTRY=$(curl -s -X POST "$GW/api/v1/tickets/entry" "${hdr[@]}" \
  -d "{\"licensePlate\":\"WB-$RUN-A\",\"vehicleType\":\"CAR\",\"strategy\":\"BEST_FIT\"}")
echo "$ENTRY" | jq .
TICKET=$(echo "$ENTRY" | jq -r .ticketNumber)

section "4. Duplicate entry for the same plate must be rejected (409)"
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$GW/api/v1/tickets/entry" "${hdr[@]}" \
  -d "{\"licensePlate\":\"WB-$RUN-A\",\"vehicleType\":\"CAR\"}"

section "5. Exit after a simulated 3h 10m stay (4 billable hours x 20 = 80.00 INR)"
curl -s -X POST "$GW/api/v1/tickets/exit" "${hdr[@]}" \
  -d "{\"ticketNumber\":\"$TICKET\",\"simulatedDurationMinutes\":190}" | jq .

section "6. Payment declined (402), then retry succeeds (200)"
curl -s -o /dev/null -w "declined -> HTTP %{http_code}\n" -X POST "$GW/api/v1/payments" "${hdr[@]}" \
  -d "{\"ticketNumber\":\"$TICKET\",\"method\":\"CARD\",\"paymentToken\":\"tok_decline\"}"
curl -s -X POST "$GW/api/v1/payments" "${hdr[@]}" \
  -d "{\"ticketNumber\":\"$TICKET\",\"method\":\"UPI\",\"paymentToken\":\"tok_ok\"}" | jq .

section "7. Ticket is PAID and the spot has been released"
curl -s "$GW/api/v1/tickets/$TICKET" | jq '{ticketNumber,status,feeAmount,spotReleased}'

section "8. Concurrency + load balancing: 20 parallel entries"
seq 1 20 | xargs -P 20 -I{} sh -c \
  "curl -s -X POST $GW/api/v1/tickets/entry -H 'Content-Type: application/json' \
     -d '{\"licensePlate\":\"WB-$RUN-C{}\",\"vehicleType\":\"CAR\"}' \
   | jq -r '[.spotCode, .spotServiceInstance] | @tsv'" > /tmp/parking-alloc-$RUN.tsv
echo "responses: $(wc -l < /tmp/parking-alloc-$RUN.tsv)"
echo "duplicate spots (MUST be empty):"; cut -f1 /tmp/parking-alloc-$RUN.tsv | sort | uniq -d
echo "requests per spot-allocation instance:"; cut -f2 /tmp/parking-alloc-$RUN.tsv | sort | uniq -c

section "9. Gateway rate limiting: 60 rapid calls (expect some HTTP 429; needs Redis)"
for i in $(seq 1 60); do curl -s -o /dev/null -w "%{http_code}\n" "$GW/api/v1/spots/available" & done | sort | uniq -c
wait
