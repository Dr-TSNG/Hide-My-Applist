#/bin/bash
SDKDIR=$ANDROID_SDK_ROOT
filename="${SDKDIR}/platforms/android-30/android.jar" #${SDKDIR}/platforms/android-30/android.jar
fileid='1f6YnnIKmke3qTQPUocCSS8d5-q7cqd-G'
filemd5='46b4ca17ea7b2372c8ce3530a731b0c3'
RETRY=0

function main(){
	if [ $RETRY -ge 3 ];then
		echo "Failed to download android-hidden-api android.jar."
		exit 1
	fi
	wget --no-check-certificate "https://drive.google.com/uc?export=download&id=${fileid}" -O ${filename}
	
	if [ $? -ne 0 -o "$filemd5" != "$(md5sum ${filename} | cut -d ' ' -f1)" ]; then
		RETRY=$[$RETRY+1]
		main
	fi
	echo "Success."
}

main
