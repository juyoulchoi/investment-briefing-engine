package com.nanum.investment.common.web;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter; import java.io.IOException; import java.util.UUID;
@Component public class TraceIdFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String traceId=request.getHeader("X-Trace-Id"); if(traceId==null||traceId.isBlank())traceId=UUID.randomUUID().toString();
  request.setAttribute(TraceIdUtils.ATTRIBUTE,traceId); response.setHeader("X-Trace-Id",traceId); chain.doFilter(request,response);
 }

}

