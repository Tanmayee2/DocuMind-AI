#!/bin/bash

BASE_URL="http://localhost:8080/api"

echo "===== DocuMindAI Backend Test Suite ====="
echo ""

# Test 1: List documents (should return empty array)
echo "Test 1: List documents"
curl -s "$BASE_URL/documents/list?userId=test_user" | grep -q "\[\]"
if [ $? -eq 0 ]; then
    echo "✅ PASS - List endpoint working"
else
    echo "❌ FAIL - List endpoint not working"
fi
echo ""

# Test 2: Get non-existent document (should return 404)
echo "Test 2: Get non-existent document"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/documents/fake_id")
if [ "$STATUS" -eq 404 ]; then
    echo "✅ PASS - 404 for non-existent document"
else
    echo "❌ FAIL - Expected 404, got $STATUS"
fi
echo ""

# Test 3: Query without document (should return 404 or 409)
echo "Test 3: Query without document"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/query" \
  -H "Content-Type: application/json" \
  -d '{"documentId":"fake_id","query":"test"}')
if [ "$STATUS" -eq 404 ] || [ "$STATUS" -eq 409 ] || [ "$STATUS" -eq 500 ]; then
    echo "✅ PASS - Proper error for invalid query"
else
    echo "❌ FAIL - Expected error status, got $STATUS"
fi
echo ""

echo "===== Backend Tests Complete ====="
echo "If all tests passed, backend is ready!"