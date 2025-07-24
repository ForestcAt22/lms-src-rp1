package jp.co.sss.lms.form;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * 勤怠フォーム
 * 
 * @author 東京ITスクール
 */
@Data
public class AttendanceForm {

	/** LMSユーザーID */
	private Integer lmsUserId;
	/** グループID */
	private Integer groupId;
	/** 年間計画No */
	private String nenkanKeikakuNo;
	/** ユーザー名 */
	private String userName;
	/** 退校フラグ */
	private Integer leaveFlg;
	/** 退校日 */
	private String leaveDate;
	/** 退校日（表示用） */
	private String dispLeaveDate;
	/** 中抜け時間(プルダウン) */
	private Map<Integer , String> blankTimes = new LinkedHashMap<>();

	// 時の選択肢
    private Map<Integer, String> hourMap = new LinkedHashMap<>(); 
    // 分の選択肢
    private Map<Integer, String> minuteMap = new LinkedHashMap<>();
	/** 日次の勤怠フォームリスト */
	@Valid
	private List<DailyAttendanceForm> attendanceList;

}
