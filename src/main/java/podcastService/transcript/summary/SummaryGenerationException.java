package podcastService.transcript.summary;

import podcastService.common.exception.BaseException;
import podcastService.common.exception.ErrorCode;

public class SummaryGenerationException extends BaseException {

    public SummaryGenerationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
