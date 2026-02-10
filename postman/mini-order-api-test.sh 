#!/usr/bin/env bash

set -e

# ==============================
# Configuration (same as Postman variables)
# ==============================
BASE_URL="http://localhost:8080"
CUSTOMER_ID="c-123"
API_KEY="super-secret-key"

# Generate Postman-like request ID
RID() {
  python3 - <<'PY'
import uuid; print("pm-" + str(uuid.uuid4()))
PY
}

echo "=============================="
echo "1. Health Check"
echo "=============================="

curl -i -X GET "$BASE_URL/health"
echo -e "\n"

echo "=============================="
echo "2. Create Order"
echo "=============================="

CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY" \
  --data-raw "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"items\": [
      { \"sku\": \"SKU1\", \"qty\": 2 },
      { \"sku\": \"SKU2\", \"qty\": 5 }
    ]
  }")

echo "$CREATE_RESPONSE"

ORDER_ID=$(echo "$CREATE_RESPONSE" | python3 - <<'PY'
import sys, json
try:
    print(json.load(sys.stdin).get("id",""))
except:
    print("")
PY
)

if [ -z "$ORDER_ID" ]; then
  echo "❌ Failed to extract ORDER_ID"
  exit 1
fi

echo "ORDER_ID=$ORDER_ID"
echo -e "\n"

echo "=============================="
echo "3. Get Order by ID"
echo "=============================="

curl -i -X GET "$BASE_URL/orders/$ORDER_ID" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY"

echo -e "\n"

echo "=============================="
echo "4. List Orders (All)"
echo "=============================="

curl -i -X GET "$BASE_URL/orders?limit=20" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY"

echo -e "\n"

echo "=============================="
echo "5. List Orders by customerId"
echo "=============================="

curl -i -X GET "$BASE_URL/orders?customerId=$CUSTOMER_ID&limit=20" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY"

echo -e "\n"

echo "=============================="
echo "6. List Orders by status"
echo "=============================="

curl -i -X GET "$BASE_URL/orders?status=CREATED&limit=20" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY"

echo -e "\n"

echo "=============================="
echo "7. Confirm Order"
echo "=============================="

curl -i -X POST "$BASE_URL/orders/$ORDER_ID/confirm" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY"

echo -e "\n"

echo "=============================="
echo "8. Process Order (May return 200 or 500)"
echo "=============================="

curl -i -X POST "$BASE_URL/orders/$ORDER_ID/process" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY"

echo -e "\n"

echo "=============================="
echo "9. Negative Test - Missing customerId"
echo "=============================="

curl -i -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY" \
  --data-raw "{
    \"items\": [
      { \"sku\": \"A1\", \"qty\": 1 }
    ]
  }"

echo -e "\n"

echo "=============================="
echo "10. Negative Test - Unknown SKU"
echo "=============================="

curl -i -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY" \
  --data-raw "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"items\": [
      { \"sku\": \"NOPE\", \"qty\": 1 }
    ]
  }"

echo -e "\n"

echo "=============================="
echo "11. Negative Test - Not Found"
echo "=============================="

curl -i -X GET "$BASE_URL/orders/non-existing-id" \
  -H "X-Request-Id: $(RID)" \
  -H "X-Api-Key: $API_KEY"

echo -e "\n"

echo "=============================="
echo "✅ Script Finished"
echo "=============================="
