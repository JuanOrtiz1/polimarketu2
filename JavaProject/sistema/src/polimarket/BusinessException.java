package polimarket;

public final class BusinessException extends RuntimeException {
    private final int status;
    public BusinessException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
    public static void require(boolean valid, int status, String message) {
        if (!valid) throw new BusinessException(status, message);
    }
}
