#!/bin/bash

SWAGGER_UI_INPUT="https://unpkg.com/swagger-ui/dist/swagger-ui-bundle.js"
SWAGGER_UI_OUTPUT="openapi/src/main/resources/templates/swagger-ui/res/swagger-ui-bundle.js"

curl -L -o "$SWAGGER_UI_OUTPUT" "$SWAGGER_UI_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $SWAGGER_UI_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

SWAGGER_UI_STANDALONE_INPUT="https://unpkg.com/swagger-ui/dist/swagger-ui-standalone-preset.js"
SWAGGER_UI_STANDALONE_OUTPUT="openapi/src/main/resources/templates/swagger-ui/res/swagger-ui-standalone-preset.js"

curl -L -o "$SWAGGER_UI_STANDALONE_OUTPUT" "$SWAGGER_UI_STANDALONE_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $SWAGGER_UI_STANDALONE_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

SWAGGER_UI_CSS_INPUT="https://unpkg.com/swagger-ui/dist/swagger-ui.css"
SWAGGER_UI_CSS_OUTPUT="openapi/src/main/resources/templates/swagger-ui/res/swagger-ui.css"

curl -L -o "$SWAGGER_UI_CSS_OUTPUT" "$SWAGGER_UI_CSS_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $SWAGGER_UI_CSS_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

OPENAPI_EXPLORER_INPUT="https://unpkg.com/openapi-explorer/dist/browser/openapi-explorer.min.js"
OPENAPI_EXPLORER_OUTPUT="openapi/src/main/resources/templates/openapi-explorer/res/openapi-explorer.min.js"

curl -L -o "$OPENAPI_EXPLORER_OUTPUT" "$OPENAPI_EXPLORER_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $OPENAPI_EXPLORER_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

REDOC_EXPLORER_INPUT="https://unpkg.com/redoc/bundles/redoc.standalone.js"
REDOC_EXPLORER_OUTPUT="openapi/src/main/resources/templates/redoc/res/redoc.standalone.js"

curl -L -o "$REDOC_EXPLORER_OUTPUT" "$REDOC_EXPLORER_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $REDOC_EXPLORER_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

RAPIDOC_EXPLORER_INPUT="https://unpkg.com/rapidoc/dist/rapidoc-min.js"
RAPIDOC_EXPLORER_OUTPUT="openapi/src/main/resources/templates/rapidoc/res/rapidoc-min.js"

curl -L -o "$RAPIDOC_EXPLORER_OUTPUT" "$RAPIDOC_EXPLORER_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $RAPIDOC_EXPLORER_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi
