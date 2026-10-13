package patients.web.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

public class ClientHeaderInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Zorg-Client";
    public static final String VALUE = "web";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (SAFE_METHODS.contains(request.getMethod()) || VALUE.equals(request.getHeader(HEADER))) {
            return true;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requests that change data need the header "
                + HEADER + ": " + VALUE);
    }
}
