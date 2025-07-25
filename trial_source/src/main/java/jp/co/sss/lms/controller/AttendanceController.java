package jp.co.sss.lms.controller;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.form.AttendanceForm;
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
	private AttendanceUtil attendanceUtil;

	/**
	* Integer 型のフィールドに対するカスタムバインディングエディタを登録
	* 空文字列をIntegerのnullとして扱う
	*/
	@InitBinder // ★このアノテーションをメソッドに追加
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (text == null || text.trim().isEmpty()) {
					setValue(null); // 空文字列の場合はnullを設定
				} else {
					try {
						setValue(Integer.valueOf(text)); // 数値に変換
					} catch (NumberFormatException e) {
						// 不正な数値形式の場合（例: "abc" など）は例外をスロー
						throw new IllegalArgumentException("Cannot convert '" + text + "' to Integer", e);
					}
				}
			}

			@Override
			public String getAsText() {
				// IntegerからStringへの変換（通常はフォーム表示時に呼び出される）
				return (getValue() != null ? getValue().toString() : "");
			}
		});
	}

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
		int notEnterCount = studentAttendanceService.getNotEnteresAttendanceCount(loginUserDto.getLmsUserId());
		model.addAttribute("notEnterCount", notEnterCount);

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
	@RequestMapping(path = "/update", method = RequestMethod.GET)
	public String update(@ModelAttribute AttendanceForm attendanceForm, Model model) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		// 勤怠フォームの生成
		AttendanceForm formWithInitialData = studentAttendanceService.setAttendanceForm(attendanceManagementDtoList);
		formWithInitialData.setBlankTimes(attendanceUtil.setBlankTime());
		formWithInitialData.setHourMap(attendanceUtil.setHourMap());
		formWithInitialData.setMinuteMap(attendanceUtil.setMinuteMap());

		model.addAttribute("attendanceForm", formWithInitialData);

		return "attendance/update";
	}

	//	@RequestMapping(path="/test", method = RequestMethod.GET)
	//	public String testPage() {
	//	    System.out.println("TEST PAGE ACCESSED!");
	//	    return "attendance/detail"; // 存在する適当なThymeleafテンプレート
	//	}
	//	
	//	@RequestMapping(path="/test", method = RequestMethod.POST)
	//	public String testPage2() {
	//	  return "redirect:/attendance/detail"; // 存在する適当なThymeleafテンプレート
	//	}

	/**
	 * 勤怠情報直接変更画面 『更新』ボタン押下
	 * 
	 * @param attendanceForm
	 * @param model
	 * @param result
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
	public String complete(@Validated @ModelAttribute("attendanceForm") AttendanceForm attendanceForm,
			BindingResult result,
			Model model) throws ParseException {
		// 更新
		if (result.hasErrors()) {
			attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
			attendanceForm.setHourMap(attendanceUtil.setHourMap());
			attendanceForm.setMinuteMap(attendanceUtil.setMinuteMap());
			model.addAttribute("attendanceForm", attendanceForm);

			List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
					.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());

			return "attendance/update"; // エラーがあれば戻る
		}

		String message = studentAttendanceService.update(attendanceForm);

		return "attendance/detail";
	}

}