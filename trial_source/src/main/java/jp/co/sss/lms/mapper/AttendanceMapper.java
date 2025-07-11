package jp.co.sss.lms.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceMapper {
	/**
	 * 現在日付より前日付で勤怠未入力数を取得する。
	 *
	 * @param currentDate 現在日付（この日付より前のレコードを対象）
	 * @param lmsUserId LMSユーザーID
	 * @param deleteFlg 削除フラグ
	 * @return 未入力の勤怠件数
	 */
	
	@Select("SELECT COUNT(*) FROM t_student_attendance " +
            "WHERE lms_user_id = #{lmsUserId} " +		//勤怠情報　lmsユーザID
			"And delete_flg = #{deleteFlg} " +		//勤怠情報　削除フラグ
            "AND training_date < #{currentDate} " +		//勤怠情報　日付
			"AND attendance_stuts != 1 " +		//勤怠情報　勤怠状態
            "AND (training_start_time IS NULL OR training_end_time IS NULL OR " +
			" training_start_time = '' OR training_end_time = '')")
    int notEnterCount(
    	@Param ("currentdate") LocalDate currentDate,
    	@Param ("lmsUserId") Integer lmsUserId,	//lmsUserIdの取得
    	@Param ("deleteFlg") Integer deleteFlg  //deleteFlgパラメータ
	);
}

