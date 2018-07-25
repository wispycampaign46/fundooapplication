package com.indream.fundoo.interceptors;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.indream.fundoo.noteservice.model.Token;
import com.indream.fundoo.util.TokenManager;

import io.jsonwebtoken.Claims;

/**
 * INTERCEPTOR LOGIC FOR TOKEN VALIDATION AND STORE AND RETREIVE OF TOKEN FROM
 * REDIS
 * 
 * @author Akshay
 *
 */
public class TokenValidatorInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = Logger.getLogger(TokenValidatorInterceptor.class);

    /**
     * TOKEN MANGER IMPLEMENTATION AUTOWIRE
     */
    @Autowired
    private TokenManager manager;

    /**
     * REDIS TEMPLATE AUTOWIRING
     */
    @Autowired
    RedisTemplate<String, Token> redisTemplate;

    /*
     * @purpose BEFORE THE CONTROLLER THS METHOD WILL EXECUTE AND PROVIDE WITH
     * NECESSARY VALUES
     *
     * @author akshay
     * 
     * @com.indream.fundoo.interceptors
     * 
     * @since Jul 24, 2018
     *
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
	    throws Exception {

	boolean status = false;

	try {
	    String token = request.getHeader("authorization");// TAKE TOKEN VALUE FROM THE HEADER
	    HttpSession httpSession = request.getSession();// CREATION OF SESSION
	    Token valueToken = null;// TOKEN CLASS REF
	    if (!redisTemplate.hasKey(token)) {// NEW ENTRY FOUND
		Claims claims = manager.validateToken(token);// PARSE THE TOKEN
		valueToken = new Token();// CREATE TOKEN OBJ
		//SETTING ALL THE REQUIRED VALUES
		valueToken.setName(claims.get("name").toString());
		valueToken.setId(claims.get("id").toString());
		valueToken.setIssuedAt(claims.getIssuedAt());
		valueToken.setIssuer(claims.getIssuer());
		//STORE K V IN REDIS
		redisTemplate.boundValueOps(token).set(valueToken, 100, TimeUnit.MINUTES);
	    }
	    valueToken = redisTemplate.boundValueOps(token).get();// GET THE TOKEN OBJECT OF TYPE TOKEN
	    httpSession.setAttribute("token", valueToken);//ON SESSION OBJECT SET TOKEN
	    System.out.println("The value of the token that is gen " + valueToken);
	    status = true;//STATUS IS TRUE FOR CONTINUE
	} catch (Exception e) {
	    LOG.error("Exception occured here ihn during the validation ");
	    LOG.error("Error message : " + e.getMessage());
	    throw new RuntimeException(e.getMessage());
	}
	LOG.info("Validation success ");
	return status;
    }

    /* @purpose
     * AFTER THE CONTROLLER RESPONS THE INTERCEPTOR TAKES THE RESPONS AND RESETS THE SESSION VALUES
     *
     * @author akshay
     * @com.indream.fundoo.interceptors
     * @since Jul 24, 2018
     *
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
	    ModelAndView modelAndView) throws Exception {
	request.getSession().invalidate();//INVALIDATE THE SESSION VALUES
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
	    throws Exception {

    }

}
