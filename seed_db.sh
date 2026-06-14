#!/bin/bash
API_URL="https://dejavu-backend-java-production.up.railway.app/api/admin"
KEY="dejavu-super-secret-admin-key"

echo "Waiting for Railway deployment to finish (polling reset-confessions)..."
while true; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE -H "X-Admin-Key: $KEY" $API_URL/reset-confessions)
  if [ "$STATUS" == "200" ]; then
    echo "Deployment is live! Database reset successful."
    break
  fi
  echo "Still waiting... (Status: $STATUS)"
  sleep 15
done

echo "Deleting any lingering user accounts just to be safe..."
curl -X DELETE -H "X-Admin-Key: $KEY" $API_URL/reset-users

declare -a THEMES=(
  "horror"
  "forbidden love"
  "deep hatred"
  "college life betrayal"
  "school life secret"
  "couple infidelity"
  "stolen money"
  "mid life crisis regret"
  "intense jealousy"
  "prank gone horribly wrong"
)

echo "Generating 20 new juicy confessions across 10 varied themes..."
for THEME in "${THEMES[@]}"; do
  echo "Generating for theme: $THEME"
  # URL encode the theme
  ENCODED_THEME=$(echo -n "$THEME" | jq -sRr @uri)
  curl -s -X POST -H "X-Admin-Key: $KEY" "$API_URL/confessions/generate-juicy?theme=$ENCODED_THEME&count=2"
  echo ""
  sleep 5
done

echo "Auto-processing all new confessions (Grading + Adding Clues)..."
curl -s -X POST -H "X-Admin-Key: $KEY" "$API_URL/confessions/force-process-all?language=English"

echo "Done!"
