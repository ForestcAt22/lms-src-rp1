package jp.co.sss.lms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jp.co.sss.lms.form.DailyAttendanceForm;

public class TimeValidator implements ConstraintValidator<ValidTime, DailyAttendanceForm> {

	@Override
	public void initialize(ValidTime constraintAnnotation) {

	}

	@Override
	public boolean isValid(DailyAttendanceForm dailyForm, ConstraintValidatorContext context) {
		if (dailyForm == null) {
			return true;
		}

		boolean isValidEntry = true; //バリデーション結果

		context.disableDefaultConstraintViolation(); //明示的に追加するカスタムメッセージのみ表示

		//時刻のnullのチェック
		boolean isStartHourNull = dailyForm.getTrainingStartHour() == null;
		boolean isStartMinuteNull = dailyForm.getTrainingStartMinute() == null;
		boolean isEndHourNull = dailyForm.getTrainingEndHour() == null;
		boolean isEndMinuteNull = dailyForm.getTrainingEndMinute() == null;

		if (isStartHourNull != isStartMinuteNull) { //片方が未入力の場合
			context.buildConstraintViolationWithTemplate("{inputinvalid}")
					.addPropertyNode("trainingStartHour") //エラーを紐付けるフィールド
//					.addProperty("trainingStartHour", "出勤時間")//メッセージの０に渡すパラメータ
					.addConstraintViolation(); //エラーを追加
			isValidEntry = false;
		}

		if (isEndHourNull != isEndMinuteNull) { //片方が未入力の場合
			context.buildConstraintViolationWithTemplate("{inputinvalid}")
					.addPropertyNode("trainingEndHour") //エラーを紐付けるフィールド
//					.addProperty("trainingEndHour", "退勤時間")//メッセージの０に渡すパラメータ
					.addConstraintViolation(); //エラーを追加
			isValidEntry = false;
		}
		
		//出勤時間と退勤時間の両方が完全に入力されている場合に、時間比較チェックを行う
		if (!isStartHourNull && !isStartMinuteNull && !isEndHourNull &&!isEndMinuteNull) {
			int startTotalMinutes = dailyForm.getTrainingStartHour() * 60 + dailyForm.getTrainingStartMinute();
			int endTotalMinutes = dailyForm.getTrainingEndHour() * 60 + dailyForm.getTrainingEndMinute();
			
			//勤務時間（出勤時間～退勤時間までの時間）を計算
//			int workingMinutes = endTotalMinute - startTotalMinute;
			
			//出勤時間に入力なし、退勤時間に入力ありの場合
		if(isStartHourNull && isStartMinuteNull && (!isEndHourNull || !isEndMinuteNull)) {
			context.buildConstraintViolationWithTemplate("{attendance.punchInEmpty}")
					.addPropertyNode("trainingStartHour") //出勤時間にエラーを紐づけ
					.addConstraintViolation();
			isValidEntry = false;
		}
		
		//出勤時間＞退勤時間の場合
		if(startTotalMinutes >= endTotalMinutes) {
			context.buildConstraintViolationWithTemplate("{attendance.trainingTimeRange}")
					.addPropertyNode("trainingEndHour")
					.addConstraintViolation();
			isValidEntry = false;
		}
		
//		//中抜け時間が勤務時間を超える場合
//		if(dailyForm.getBlankTime() != null && dailyForm.getBlankTime() > workingMinutes) {
//			context.buildConstraintViolationWithTemplate("attendance.blankTimeError")
//					.addPropertyNode("blankTime")
//					.addConstraintViolation();
//			isValidEntry = false;
//		}
			
		}
		return isValidEntry; //チェックが成功すればtrue
	}
}