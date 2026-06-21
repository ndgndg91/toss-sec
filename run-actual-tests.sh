#!/bin/bash

# .env.local 파일이 존재하면 로드
if [ -f .env.local ]; then
  echo "Loading environment variables from .env.local..."
  # 주석(#)을 제외하고 빈 줄을 무시하여 export
  export $(grep -v '^#' .env.local | xargs)
fi

show_usage() {
  echo "Usage: $0 [mail|api|all]"
  echo "  mail : 실제 메일 발송 테스트 실행"
  echo "  api  : 실제 Toss API 연동 테스트 실행"
  echo "  all  : 두 테스트 모두 실행"
}

if [ -z "$1" ]; then
  show_usage
  exit 1
fi

case "$1" in
  mail)
    if [ -z "$SPRING_MAIL_PASSWORD" ]; then
      echo "Error: SPRING_MAIL_PASSWORD가 설정되지 않았습니다. .env.local 파일을 구성하거나 환경 변수를 주입하세요."
      exit 1
    fi
    ./gradlew clean test --tests "com.giri.trader.application.MailServiceActualSendTest"
    ;;
  api)
    if [ -z "$TOSS_SEC_API_KEY" ] || [ -z "$TOSS_SEC_SECRET_KEY" ]; then
      echo "Error: TOSS_SEC_API_KEY 또는 TOSS_SEC_SECRET_KEY가 설정되지 않았습니다. .env.local 파일을 구성하거나 환경 변수를 주입하세요."
      exit 1
    fi
    ./gradlew clean test --tests "com.giri.trader.infrastructure.toss.TossApiClientActualTest"
    ;;
  all)
    ./gradlew clean test \
      --tests "com.giri.trader.application.MailServiceActualSendTest" \
      --tests "com.giri.trader.infrastructure.toss.TossApiClientActualTest"
    ;;
  *)
    show_usage
    exit 1
    ;;
esac
