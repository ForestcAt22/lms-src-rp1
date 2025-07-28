package jp.co.sss.lms.validation;

import jakarta.validation.ConstraintValidator; // ここは「jakarta」であることを再確認！
import jakarta.validation.ConstraintValidatorContext;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.util.TrainingTime; // util パッケージが存在するならこのimportでOK

public class TimeValidator implements ConstraintValidator<ValidTime, DailyAttendanceForm> {

    @Override
    public void initialize(ValidTime constraintAnnotation) {
        // 初期化が必要な場合はここに記述
    }

    @Override
    public boolean isValid(DailyAttendanceForm dailyForm, ConstraintValidatorContext context) {
        if (dailyForm == null) {
            return true;
        }

        boolean isValidEntry = true;
        context.disableDefaultConstraintViolation();

        // 時刻のnullチェック
        boolean isStartHourNull = dailyForm.getTrainingStartHour() == null;
        boolean isStartMinuteNull = dailyForm.getTrainingStartMinute() == null;
        boolean isEndHourNull = dailyForm.getTrainingEndHour() == null;
        boolean isEndMinuteNull = dailyForm.getTrainingEndMinute() == null;

        if (isStartHourNull != isStartMinuteNull) { // 片方が未入力の場合
            context.buildConstraintViolationWithTemplate("{inputinvalid}")
                    .addPropertyNode("trainingStartHour")
                    .addConstraintViolation();
            isValidEntry = false;
        }

        if (isEndHourNull != isEndMinuteNull) { // 片方が未入力の場合
            context.buildConstraintViolationWithTemplate("{inputinvalid}")
                    .addPropertyNode("trainingEndHour")
                    .addConstraintViolation();
            isValidEntry = false;
        }

        TrainingTime startTime = null;
        if (!isStartHourNull && !isStartMinuteNull) {
            startTime = new TrainingTime(dailyForm.getTrainingStartHour(), dailyForm.getTrainingStartMinute());
        }

        TrainingTime endTime = null;
        if (!isEndHourNull && !isEndMinuteNull) {
            endTime = new TrainingTime(dailyForm.getTrainingEndHour(), dailyForm.getTrainingEndMinute());
        }

        // 出勤時間に入力なし、退勤時間に入力ありの場合
        if ((isStartHourNull || isStartMinuteNull) && (endTime != null)) {
            context.buildConstraintViolationWithTemplate("{attendance.punchInEmpty}")
                    .addPropertyNode("trainingStartHour")
                    .addConstraintViolation();
            isValidEntry = false;
        }

        // 出勤時間＞退勤時間の場合
        if (startTime != null && endTime != null) {
            if (startTime.compareTo(endTime) >= 0) {
                context.buildConstraintViolationWithTemplate("{attendance.trainingTimeRange}")
                        .addPropertyNode("trainingEndHour")
                        .addConstraintViolation();
                isValidEntry = false; // ここに設定

            // 中抜け時間が勤務時間を超える場合
            } else if (dailyForm.getBlankTime() != null && dailyForm.getBlankTime() > 0) { // else if に変更
                // 勤務時間を計算(退勤時間 - 出勤時間）
                int startTotalMinutes = startTime.getHour() * 60 + startTime.getMinute();
                int endTotalMinutes = endTime.getHour() * 60 + endTime.getMinute();
                int workingMinutes = endTotalMinutes - startTotalMinutes;

                // 退勤が出勤より後かつ中抜け時間が勤務時間を超える場合
                if (workingMinutes > 0 && dailyForm.getBlankTime() > workingMinutes) {
                    context.buildConstraintViolationWithTemplate("{attendance.blankTimeError}")
                            .addPropertyNode("blankTime")
                            .addConstraintViolation();
                    isValidEntry = false;
                }
            }
        }
        System.out.println("--- Debugging TimeValidator Errors (End of isValid method) ---");
        System.out.println("isValidEntry (before return): " + isValidEntry); // 最終的なバリデーション結果

        // ConstraintValidatorContext から直接エラーメッセージを取得するのは少し手間がかかりますが、
        // isValid() が false を返す場合は、エラーが登録されています。
        // ここでは Spring の BindingResult のように直接エラーメッセージリストを取得できないため、
        // isValidEntry の値で判断します。
        // 実際には、このバリデーターが呼び出された後の BindingResult を見るのが最も確実です。

        // 便宜上、引数で渡されたdailyFormの中身を再度ログ出力してみます
        System.out.println("DailyAttendanceForm trainingStartHour: " + dailyForm.getTrainingStartHour());
        System.out.println("DailyAttendanceForm trainingStartMinute: " + dailyForm.getTrainingStartMinute());
        System.out.println("DailyAttendanceForm trainingEndHour: " + dailyForm.getTrainingEndHour());
        System.out.println("DailyAttendanceForm trainingEndMinute: " + dailyForm.getTrainingEndMinute());
        System.out.println("DailyAttendanceForm blankTime: " + dailyForm.getBlankTime());
        System.out.println("--- End Debugging TimeValidator Errors ---");
        return isValidEntry;
    }
}