package jp.co.sss.lms.form;

import jakarta.validation.constraints.Size;
import jp.co.sss.lms.validation.ValidTime;
import lombok.Data;

/**
 * 日次の勤怠フォーム
 * 
 * @author 東京ITスクール
 */
@Data
@ValidTime
public class DailyAttendanceForm {

	/** 受講生勤怠ID */
	private Integer studentAttendanceId;
	/** 途中退校日 */
	private String leaveDate;
	/** 日付 */
	private String trainingDate;
	/** 出勤時間 */
	private String trainingStartTime;
	/** 出勤時刻（時) */
//	@NotNull(message="{0}は必須です。",groups = ValidationGroup.class)
	private Integer trainingStartHour;
	/** 出勤時間（分）*/
//	@NotNull(message="{0}は必須です。",groups = ValidationGroup.class)
	private Integer trainingStartMinute;
	/** 退勤時間 */
	private String trainingEndTime;
	/** 退勤時間（時）*/
//	@NotNull(message="{0}は必須です。",groups = ValidationGroup.class)
	private Integer trainingEndHour;
	/** 退勤時間（分）*/
//	@NotNull(message="{0}は必須です。",groups = ValidationGroup.class)
	private Integer trainingEndMinute;
	/** 中抜け時間 */
	private Integer blankTime;
	/** 中抜け時間（画面表示用） */
	private String blankTimeValue;
	/** ステータス */
	private String status;
	/** 備考 */
	@Size(max=100,message="１００文字以内で入力してください")
	private String note;
	/** セクション名 */
	private String sectionName;
	/** 当日フラグ */
	private Boolean isToday;
	/** エラーフラグ */
	private Boolean isError;
	/** 日付（画面表示用） */
	private String dispTrainingDate;
	/** ステータス（画面表示用） */
	private String statusDispName;
	/** LMSユーザーID */
	private String lmsUserId;
	/** ユーザー名 */
	private String userName;
	/** コース名 */
	private String courseName;
	/** インデックス */
	private String index;

}
