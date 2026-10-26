#!/bin/sh
set -eu

BUCKET_NAME="${AWS_S3_BUCKET:-saga-ecommerce-products}"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"

echo "Preparando bucket S3 local: ${BUCKET_NAME}"
awslocal s3api head-bucket --bucket "${BUCKET_NAME}" >/dev/null 2>&1 ||
  awslocal s3 mb "s3://${BUCKET_NAME}" --region "${REGION}"

echo "Bucket ${BUCKET_NAME} pronto."
