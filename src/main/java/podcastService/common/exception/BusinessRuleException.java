package podcastService.common.exception;

import java.util.Map;

public class BusinessRuleException extends BaseException {
    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessRuleException(String message, Map<String, Object> details) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message, details);
    }
}
