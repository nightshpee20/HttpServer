package util;

public enum StatusCodes {
	NOT_FOUND("404 NOT FOUND"), 
    OK("200 OK"), 
    FORBIDDEN("403 FORBIDDEN");
 
	private String code;

	StatusCodes(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}