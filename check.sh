#!/bin/bash
echo "🔍 ĐANG QUÉT LỖI MÃ NGUỒN KOTLIN (Vui lòng đợi 1-2 phút)..."
chmod +x ./gradlew

# Chạy lệnh biên dịch Kotlin, bỏ qua lỗi C++ (dùng cờ --continue)
./gradlew :app:compileDebugKotlin --continue --no-daemon > local_build_log.txt 2>&1

# Tìm kiếm các dòng báo lỗi của trình biên dịch Kotlin (bắt đầu bằng "e: file")
if grep -q "e: file" local_build_log.txt; then
    echo -e "\n❌ PHÁT HIỆN LỖI CODE KOTLIN! Vui lòng sửa các lỗi sau trước khi push:"
    echo "--------------------------------------------------------"
    # In ra màn hình các dòng lỗi với màu đỏ để dễ nhìn
    grep "e: file" local_build_log.txt | sed $'s/e: /\\\e[31m[LỖI] \\\e[0m/'
    echo "--------------------------------------------------------"
    exit 1
else
    echo -e "\n✅ HOÀN HẢO! Mã Kotlin không có lỗi cú pháp."
    echo "🚀 Bạn có thể tự tin chạy lệnh: git commit và git push"
    exit 0
fi
