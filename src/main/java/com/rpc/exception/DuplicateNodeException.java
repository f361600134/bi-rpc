package com.rpc.exception;

/**
 * 服务器节点重复注册异常
 * @author Jeremy
 */
public class DuplicateNodeException extends RuntimeException {

	private static final long serialVersionUID = -6409836265505953209L;

	public DuplicateNodeException() {
		super();
	}

	public DuplicateNodeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DuplicateNodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateNodeException(String message) {
		super(message);
	}

	public DuplicateNodeException(Throwable cause) {
		super(cause);
	}

}
