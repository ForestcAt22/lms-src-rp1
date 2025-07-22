package jp.co.sss.lms.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.validation.Valid;
import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.AttendanceMapper;
import jp.co.sss.lms.service.StudentAttendanceService;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;

/**
 * 勤怠管理コントローラ
 * 
 * @author 東京ITスクール
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	@Autowired
	private StudentAttendanceService studentAttendanceService;
	@Autowired
	private LoginUserDto loginUserDto;

	@Autowired
	private AttendanceMapper attenDanceMapper;
	@Autowired
	private AttendanceUtil attendanceUtil;

	/**
	 * 勤怠管理画面 初期表示
	 * 
	 * @param lmsUserId
	 * @param courseId
	 * @param model
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/detail", method = RequestMethod.GET)
	public String index(Model model) {

		// 勤怠一覧の取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		//未入力有無の判定を行う
		int notEnterCount =studentAttendanceService.getNotEnteresAttendanceCount(loginUserDto.getLmsUserId());
		boolean hasNotEntries = notEnterCount > 0;
		model.addAttribute("hasNotEntries", hasNotEntries);
		
		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『出勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
	public String punchIn(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_ATWORK);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchIn();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『退勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
	public String punchOut(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_LEAVING);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchOut();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『勤怠情報を直接編集する』リンク押下
	 * 
	 * @param model
	 * @return 勤怠情報直接変更画面
	 */
	@GetMapping("/update")
	public String update(@ModelAttribute AttendanceForm attendanceForm ,BindingResult result , Model model) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		// 勤怠フォームの生成
		AttendanceForm formWithInitialData = studentAttendanceService.setAttendanceForm(attendanceManagementDtoList);
		model.addAttribute("attendanceForm", formWithInitialData);
		model.addAttribute("BlankTimes", attendanceUtil.setBlankTime());

		return "attendance/update";
	}

	/**
	 * 勤怠情報直接変更画面 『更新』ボタン押下
	 * 
	 * @param attendanceForm
	 * @param model
	 * @param result
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@PostMapping("/update")
	public String complete(@Valid @ModelAttribute AttendanceForm attendanceForm, Model model, BindingResult result)
			throws ParseException {
		
		//カスタムチェック
		for(int i =0; i < attendanceForm.getAttendanceList().size();i++) {
			DailyAttendanceForm dailyForm = attendanceForm.getAttendanceList().get(i);
			
			//出勤時間のチェック
			boolean isStartHourNull = dailyForm.getTrainingStartHour() == null;
			boolean isStartMinuteNull = dailyForm.getTrainingStartMinute() == null;
			
			if(isStartHourNull != isStartMinuteNull) { //片方のみ
				result.addError(new FieldError("attendanceForm" , "attendanceList[" +i+ "].trainingStartHour",
						"出勤時間が正しく入力されていません。"));
			}
			//退勤時間のチェック
			boolean isEndHourNull = dailyForm.getTrainingEndHour() == null;
			boolean isEndMinuteNull = dailyForm.getTrainingEndMinute() == null;
			
			if(isEndHourNull != isEndMinuteNull) { //片方のみ
				result.addError(new FieldError("attendanceForm" , "attendanceList[" +i+ "].trainingEndHour",
						"退勤時間が正しく入力されていません。"));
			}
		}

		// 更新
		if (result.hasErrors()) {
			model.addAttribute("attendanceForm", attendanceForm);
			model.addAttribute("blankTimes", attendanceUtil.setBlankTime()); //中抜け時間のプルダウン再設定
			//エラーメッセージ　デバック出力(開発時)
			result.getAllErrors()
					.forEach(error -> {
						String fieldName = "";
						if (error instanceof org.springframework.validation.FieldError) {
							fieldName = ((org.springframework.validation.FieldError) error).getField();
						}

						System.err.println(
								"ValidationError:" + error.getDefaultMessage() + "on field:" + error.getObjectName()
										+ "."
										+ fieldName);
					});
			return "attendance/update"; //エラーがあれば戻る
		}

		String message = studentAttendanceService.update(attendanceForm);
		model.addAttribute("message", message);
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

}