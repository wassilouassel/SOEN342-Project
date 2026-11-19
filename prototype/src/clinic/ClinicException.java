package clinic;

/** Expected, user-readable domain rejection. */
public final class ClinicException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String code;
    public ClinicException(String code, String message) {
        super(message);
        this.code = code;
    }
    public String code() { return code; }
}
