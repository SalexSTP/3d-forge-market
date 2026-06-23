package com.aleksandar.threedforgemarket.web.common;

import com.aleksandar.threedforgemarket.exception.auth.EmailAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.auth.PasswordsDoNotMatchException;
import com.aleksandar.threedforgemarket.exception.auth.UserNotFoundException;
import com.aleksandar.threedforgemarket.exception.auth.UsernameAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.order.CustomerOrderNotFoundException;
import com.aleksandar.threedforgemarket.exception.order.OrderCancellationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderStatusUpdateNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.ProductUnavailableException;
import com.aleksandar.threedforgemarket.exception.product.ProductDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.product.ProductNameAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.exception.review.ReviewAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.review.ReviewCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.review.ReviewEligibilityNotMetException;
import com.aleksandar.threedforgemarket.exception.review.ReviewNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ProductNotFoundException.class,
            CustomerOrderNotFoundException.class,
            ReviewNotFoundException.class,
            UserNotFoundException.class,
            NoResourceFoundException.class
    })
    public ModelAndView handleNotFound() {
        return errorView("error/404", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ModelAndView handleBadRequest() {
        return errorView("error/400", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleMethodNotAllowed() {
        return errorView("error/405", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler({
            OrderCreationNotAllowedException.class,
            ReviewCreationNotAllowedException.class
    })
    public ModelAndView handleForbiddenDomainAction() {
        return errorView("error/403", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
            EmailAlreadyExistsException.class,
            PasswordsDoNotMatchException.class,
            UsernameAlreadyExistsException.class,
            OrderCancellationNotAllowedException.class,
            OrderDeletionNotAllowedException.class,
            OrderStatusUpdateNotAllowedException.class,
            ProductUnavailableException.class,
            ProductDeletionNotAllowedException.class,
            ProductNameAlreadyExistsException.class,
            ReviewAlreadyExistsException.class,
            ReviewEligibilityNotMetException.class
    })
    public ModelAndView handleDomainConflict() {
        return errorView("error/409", HttpStatus.CONFLICT);
    }

    private ModelAndView errorView(String viewName, HttpStatus status) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.setStatus(status);

        return modelAndView;
    }
}
