package com.macro.mall.config;

import com.macro.mall.bo.AdminUserDetails;
import com.macro.mall.component.JwtAuthenticationTokenFilter;
import com.macro.mall.component.RestAuthenticationEntryPoint;
import com.macro.mall.component.RestfulAccessDeniedHandler;
import com.macro.mall.model.UmsAdmin;
import com.macro.mall.model.UmsPermission;

import com.macro.mall.sys.service.UmsAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;


/**
 * SpringSecurity的配置
 * https://gitee.com/zscat-platform/mall on 2018/4/26.
 */
@Configuration
@EnableWebSecurity
//通过 @EnableWebSecurity注解开启Spring Security的功能
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@EnableGlobalMethodSecurity(prePostEnabled = true)这个注解，可以开启security的注解，我们可以在需要控制权限的方法上面使用@PreAuthorize，@PreFilter这些注解。
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private UmsAdminService adminService;
    @Autowired
    private RestfulAccessDeniedHandler restfulAccessDeniedHandler;
    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf().disable()// crsf禁用,由于使用的是JWT，不是session,我们这里不需要csrf
                .sessionManagement()// 基于token，所以不需要session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                
//                .authorizeRequests()
//                .antMatchers("/image/**").permitAll() //静态资源访问无需认证
//                .antMatchers("/admin/**").hasAnyRole("ADMIN") //admin开头的请求，需要admin权限
//                .antMatchers("/article/**").hasRole("USER") //需登陆才能访问的url
//                .anyRequest().authenticated()  //默认其它的请求都需要认证，这里一定要添加
//                .and()
//            .csrf().disable()  //CRSF禁用，因为不使用session
//            .sessionManagement().disable()  //禁用session
//            .formLogin().disable() //禁用form登录
//            .cors()  //支持跨域
//            .and()   //添加header设置，支持跨域和ajax请求
//            .headers().addHeaderWriter(new StaticHeadersWriter(Arrays.asList(
//                    new Header("Access-control-Allow-Origin","*"),
//                    new Header("Access-Control-Expose-Headers","Authorization"))))
//            .and() //拦截OPTIONS请求，直接返回header
//            .addFilterAfter(new OptionRequestFilter(), CorsFilter.class)
//            //添加登录filter
//            .apply(new JsonLoginConfigurer<>()).loginSuccessHandler(jsonLoginSuccessHandler())
//            .and()
//           //添加token的filter
//            .apply(new JwtLoginConfigurer<>()).tokenValidSuccessHandler(jwtRefreshSuccessHandler()).permissiveRequestUrls("/logout")
//            .and()
//            //使用默认的logoutFilter
//            .logout()
////              .logoutUrl("/logout")   //默认就是"/logout"
//                .addLogoutHandler(tokenClearLogoutHandler())  //logout时清除token
//                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()) //logout成功后返回200
//            .and()
//            .sessionManagement().disable();
                
                
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, // 允许对于网站静态资源的无授权访问
                        "/",
                        "/*.html",
                        "/favicon.ico",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/swagger-resources/**",
                        "/v2/api-docs/**"
                ).permitAll()
                .antMatchers("/admin/login", "/admin/register")// 对登录注册要允许匿名访问
                .permitAll()
                .antMatchers(HttpMethod.OPTIONS)//跨域请求会先进行一次options请求
                .permitAll()
                .antMatchers("/**")//测试时全部运行访问
                .permitAll()
                .anyRequest().authenticated();// 除上面外的所有请求全部需要鉴权认证
        // 禁用缓存
        httpSecurity.headers().cacheControl();
        // 添加JWT filter
        httpSecurity.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        //添加自定义未授权和未登录结果返回
        httpSecurity.exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler)
                .authenticationEntryPoint(restAuthenticationEntryPoint);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService())
                .passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Md5PasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        //获取登录用户信息
        return username -> {
            UmsAdmin admin = adminService.getAdminByUsername(username);
            if (admin != null) {
                List<UmsPermission> permissionList = adminService.getPermissionList(admin.getId());
                return new AdminUserDetails(admin, permissionList);
            }
            throw new UsernameNotFoundException("用户名或密码错误");
        };
    }

    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() {
        return new JwtAuthenticationTokenFilter();
    }

    /**
     * 允许跨域调用的过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(0);
        return new CorsFilter(source);
    }
}
