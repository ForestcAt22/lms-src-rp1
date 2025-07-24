package jp.co.sss.lms.service;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	private static final Logger logger = Logger.getLogger(StudentAttendanceService.class.getName());

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 過去の勤怠未入力件数を取得します。
	 *
	 * @param lmsUserId 対象のLMSユーザーID
	 * @return 未入力勤怠の件数
	 */
	public int getNotEnteresAttendanceCount(Integer lmsUserId) {
		LocalDate today = LocalDate.now();
		int deleteFlg = Constants.DB_FLG_FALSE;

		return tStudentAttendanceMapper.notEnterCount(today, lmsUserId, deleteFlg);
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	* 日次勤怠フォームの出勤・退勤時間の未入力チェック
	* @param attendanceList DailyAttendanceFormのリスト
	* @param result BindingResult
	*/
	//    public void validateDailyAttendanceTimes(List<DailyAttendanceForm> attendanceList, BindingResult result) {
	//        for (int i = 0; i < attendanceList.size(); i++) {
	//            DailyAttendanceForm dailyForm = attendanceList.get(i);
	//
	//            // 出勤時間のチェック
	//            boolean isStartHourNull = dailyForm.getTrainingStartHour() == null;
	//            boolean isStartMinuteNull = dailyForm.getTrainingStartMinute() == null;
	//
	//            if (isStartHourNull != isStartMinuteNull) { // 片方のみが未入力の場合
	//                result.addError(new FieldError("attendanceForm", "attendanceList[" + i + "].trainingStartHour",
	//                        "出勤時間が正しく入力されていません。"));
	//            }
	//
	//            // 退勤時間のチェック
	//            boolean isEndHourNull = dailyForm.getTrainingEndHour() == null;
	//            boolean isEndMinuteNull = dailyForm.getTrainingEndMinute() == null;
	//
	//            if (isEndHourNull != isEndMinuteNull) { // 片方のみが未入力の場合
	//                result.addError(new FieldError("attendanceForm", "attendanceList[" + i + "].trainingEndHour",
	//                        "退勤時間が正しく入力されていません。"));
	//            }
	//        }
	//    }

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());

		Map<Integer, String> blankTimesMap = attendanceUtil.setBlankTime();
		if (blankTimesMap == null) {
			attendanceForm.setBlankTimes(new LinkedHashMap<>()); // ユーティリティが null を返す場合、空のMapを設定
		} else {
			attendanceForm.setBlankTimes(blankTimesMap);
		}

		//時と分のプルダウン設定
		attendanceForm.setHourMap(attendanceUtil.setHourMap());
		attendanceForm.setMinuteMap(attendanceUtil.setMinuteMap());

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			//			dailyAttendanceForm
			//					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			//			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			//				dailyAttendanceForm.setTrainingStartHour(attendanceUtil.getHourFromTime(attendanceManagementDto.getTrainingStartTime()));
			//				dailyAttendanceForm.setTrainingStartMinute(attendanceUtil.getMinuteFromTime(attendanceManagementDto.getTrainingStartTime()));
			//				dailyAttendanceForm.setTrainingEndHour(attendanceUtil.getHourFromTime(attendanceManagementDto.getTrainingEndTime()));
			//				dailyAttendanceForm.setTrainingEndMinute(attendanceUtil.getMinuteFromTime(attendanceManagementDto.getTrainingEndTime()));;
			//			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			if (attendanceManagementDto.getTrainingStartTime() != null
					&& !attendanceManagementDto.getTrainingStartTime().isEmpty()) {
				try {
					LocalTime startTime = LocalTime.parse(attendanceManagementDto.getTrainingStartTime());
					dailyAttendanceForm.setTrainingStartHour(startTime.getHour());
					dailyAttendanceForm.setTrainingStartMinute(startTime.getMinute());
				} catch (DateTimeParseException e) {
					logger.warning("TrainingStartTimeのパースエラー ( ID:" + attendanceManagementDto.getStudentAttendanceId()
							+ "):" + e.getMessage());
					dailyAttendanceForm.setTrainingStartHour(null);
					dailyAttendanceForm.setTrainingStartMinute(null);
				}
			} else {
				dailyAttendanceForm.setTrainingStartHour(null);
				dailyAttendanceForm.setTrainingStartMinute(null);
			}

			if (attendanceManagementDto.getTrainingEndTime() != null
					&& !attendanceManagementDto.getTrainingEndTime().isEmpty()) {
				try {
					LocalTime endTime = LocalTime.parse(attendanceManagementDto.getTrainingEndTime());
					dailyAttendanceForm.setTrainingEndHour(endTime.getHour());
					dailyAttendanceForm.setTrainingEndMinute(endTime.getMinute());
				} catch (DateTimeParseException e) {
					logger.warning("TrainingEndTimeのパースエラー ( ID:" + attendanceManagementDto.getStudentAttendanceId()
							+ "):" + e.getMessage());
					dailyAttendanceForm.setTrainingEndHour(null);
					dailyAttendanceForm.setTrainingEndMinute(null);
				}
			} else {
				dailyAttendanceForm.setTrainingEndHour(null);
				dailyAttendanceForm.setTrainingEndMinute(null);

			}

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		Map<Date, TStudentAttendance> existingAttendanceMap = new LinkedHashMap<>();
		List<TStudentAttendance> currentStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);
		for (TStudentAttendance entity : currentStudentAttendanceList) {
			existingAttendanceMap.put(entity.getTrainingDate(), entity);
		}
		List<TStudentAttendance> entitiesToSave = new ArrayList<>();
		// 入力された情報を更新用のエンティティに移し替え
		Date currentDate = new Date();

		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			Date trainingDate = dateUtil.parse(dailyAttendanceForm.getTrainingDate());
			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = existingAttendanceMap.get(trainingDate);
			// 日次勤怠フォームから更新用のエンティティにコピー
			//	BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			if (tStudentAttendance == null) {
				tStudentAttendance = new TStudentAttendance();
				tStudentAttendance.setTrainingDate(trainingDate);
				tStudentAttendance.setLmsUserId(lmsUserId);
				tStudentAttendance.setAccountId(loginUserDto.getAccountId());
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(currentDate);
			} else {
				//既存レコードの場合
				tStudentAttendance.setAccountId(loginUserDto.getAccountId());
				;
			}

			TrainingTime trainingStartTime = null;
			if (dailyAttendanceForm.getTrainingStartHour() != null
					&& dailyAttendanceForm.getTrainingStartMinute() != null) {
				try {
					LocalTime localStartTime = LocalTime.of(dailyAttendanceForm.getTrainingStartHour(),
							dailyAttendanceForm.getTrainingStartMinute());
					trainingStartTime = new TrainingTime(localStartTime.getHour(), localStartTime.getMinute());
					tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
				} catch (Exception e) {
					logger.warning("出勤時刻のTrainingTime生成エラー (ID：" + dailyAttendanceForm.getStudentAttendanceId()
							+ ")：" + e.getMessage());
					tStudentAttendance.setTrainingStartTime(""); //エラー時は空文字
				}
			} else {
				tStudentAttendance.setTrainingStartTime(""); //時刻が完全に入力されていない場合
			}

			TrainingTime trainingEndTime = null;
			if (dailyAttendanceForm.getTrainingEndHour() != null
					&& dailyAttendanceForm.getTrainingEndMinute() != null) {
				try {
					LocalTime localEndTime = LocalTime.of(dailyAttendanceForm.getTrainingEndHour(),
							dailyAttendanceForm.getTrainingEndMinute());
					trainingEndTime = new TrainingTime(localEndTime.getHour(), localEndTime.getMinute());
					tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
				} catch (Exception e) {
					logger.warning("退勤時刻のTrainingTime生成エラー (ID:" + dailyAttendanceForm.getStudentAttendanceId()
							+ ")：" + e.getMessage());
					tStudentAttendance.setTrainingEndTime("");
				}
			} else {
				tStudentAttendance.setTrainingEndTime(""); //時刻が入力されていない
			}

			//			// 出勤時刻整形
			//			TrainingTime trainingStartTime = null;
			//			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
			//			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			//			// 退勤時刻整形
			//			TrainingTime trainingEndTime = null;
			//			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
			//			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if (tStudentAttendance.getTrainingStartTime().isEmpty()
					&& tStudentAttendance.getTrainingEndTime().isEmpty()) {
				if (AttendanceStatusEnum.ABSENT.name.equals(dailyAttendanceForm.getStatusDispName())) {
					//時刻が空の場合、欠席を保持
					tStudentAttendance.setStatus(AttendanceStatusEnum.ABSENT.code);
				} else {
					//時刻が空で欠席でない場合未入力を設定
					tStudentAttendance.setStatus(AttendanceStatusEnum.NONE.code);
				}
			} else {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
						trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(currentDate);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
	
			entitiesToSave.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance entity : entitiesToSave) {
			if(entity.getStudentAttendanceId() == null) {
				tStudentAttendanceMapper.insert(entity);
			} else {
				tStudentAttendanceMapper.update(entity);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

}
