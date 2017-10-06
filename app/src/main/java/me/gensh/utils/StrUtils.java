package me.gensh.utils;

import android.util.Base64;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import me.gensh.natives.Encrypt;

public class StrUtils {
    static String path;
    final public static int IV_LENGTH = 10;

    /**
     * @param plainTextString the string needed to be encrypted.
     * @param iv              A nonce N of 15-L octets(in CCMP,L=2).  Within the scope of any encryption key,the nonce value MUST be unique. from RFC 3610
     * @return the string after encrypt with base64 encode.
     */
    public static String encryptWithIv(String plainTextString, byte[] iv) {
        byte[] result = Encrypt.nativeEncrypt(plainTextString.getBytes(), iv);
        return Base64.encodeToString(result, Base64.DEFAULT);
    }

    /**
     * @param cipherTextBase64 the string(with base64 encoded) needed to be encrypted
     * @param iv               A nonce N of 15-L octets(in CCMP,L=2).
     * @return the string after decrypt
     */
    public static String decryptWithIv(String cipherTextBase64, byte[] iv) {
        byte[] result = Encrypt.nativeDecrypt(Base64.decode(cipherTextBase64, Base64.DEFAULT), iv);
        return new String(result);
    }

    /**
     * generate a byte array with random content.
     *
     * @param len the array length
     */
    public static byte[] randomByteArray(int len) {
        byte[] arr = new byte[len];
        Random random = new Random();
        random.nextBytes(arr);
        return arr;
    }

    //convert a String to a int value.
    public static int parseInt(String str, int defaultValue) {
        try {
            int v;
            v = Integer.parseInt(str);
            return v;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String[] split2(String str, char ch1, char ch2) {
        char[] Arr = str.toCharArray();
        for (int i = 0; i < Arr.length; i++)
            if (Arr[i] == ch1 || Arr[i] == ch2)
                Arr[i] = '@';
        return String.valueOf(Arr).split("@");
    }

    //关于课程表的两个函数；
    public static String getCourseShareStr(List<HashMap<String, Object>> mapList) {
        String result = "【分享课表】今日的课程有：\n";
        int length = mapList.size();
        for (int i = 0; i < length; i++) {
            HashMap<String, Object> map = mapList.get(i);
            result += addCourse(map);
        }
        return result;
    }

    private static String addCourse(HashMap<String, Object> map) {
        //lesson_no,times,course_name,place,teachers
        String returnStr = "第" + map.get("lesson_no") + "节课\t";
        returnStr += map.get("times") + "\t";
        returnStr += map.get("course_name") + "\t";
        returnStr += map.get("place") + "\t";
        returnStr += map.get("teachers") + "\n";
        return returnStr;
    }


    //	Lesson.activity的处理课程表
//	public static void getCourse(int[] id, Cursor cursor, String[] detail) {
//		int k = -1;
//		//int[] id_copy = {0,0,0,0,0,0};
//		String[] detail_copy = {"","","","","","",};
//
//		if(cursor.moveToFirst()){
//			getDeatil(k,detail_copy,cursor);
//		}
//		while(cursor.moveToNext()){
//			getDeatil(k,detail_copy,cursor);
//		}
//		for(int i=0;i<6;i++){
//			//id[i] = id_copy[i];
//			detail [i] = detail_copy[i];
//		}
//		cursor.close();
//	}
//
//	private static void getDeatil(int k,String[] detail_copy,Cursor cursor){
//		int k1 = cursor.getInt(cursor.getColumnIndex("lesson_no"))-1;
//		//id_copy [k1]=k1+1;
//		String LessonStr = "";
//		LessonStr += cursor.getString(cursor.getColumnIndex("course_name"))+"\n";
//		LessonStr += cursor.getString(cursor.getColumnIndex("place"))+"\n";
//		LessonStr += cursor.getString(cursor.getColumnIndex("teachers"))+"\n";
//		LessonStr += "第"+cursor.getString(cursor.getColumnIndex("weeks"))+"\n";
//		LessonStr += cursor.getString(cursor.getColumnIndex("course_type"))+"\n";
//
//		detail_copy[k1] += LessonStr+"\n";
//	}

    //MainActivity title setting
//	public static int getActionBarTitle(int number) {
//
//		switch (number) {
//			case 1:
//				return R.string.left_side_home;
//			case 2:
//				return R.string.left_side_record_query;
//			case 3:
//				return R.string.left_side_elective_system;
//			case 4:
//				return R.string.left_side_my_schedule;
//			case 5:
//				return R.string.left_side_volunteer;
//			case 6:
//				return R.string.left_side_library;
//			case 7:
//				return R.string.left_side_campus_network;
//			case 8:
//				return R.string.setting;
//		}
//		return R.string.left_side_home;
//	}

}
