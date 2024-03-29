package com.hhf.alaska.spring.boot.starter.zuul.common;

import com.hhf.alaska.spring.boot.starter.zuul.common.rule.IZuulRouteRule;
import com.hhf.alaska.spring.boot.starter.zuul.common.rule.IZuulRouteRuleMatcher;
import com.hhf.alaska.spring.boot.starter.zuul.common.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author huang hong fei
 */
public abstract class ZuulRouteLocator extends SimpleRouteLocator implements RefreshableRouteLocator {

	public final static Logger logger = LoggerFactory.getLogger(ZuulRouteLocator.class);
	
	private ZuulProperties properties;
	
	@Autowired
	private IZuulRouteRuleMatcher zuulRouteRuleMatcher;
	
	public ZuulRouteLocator(String servletPath, ZuulProperties properties) {
		super(servletPath, properties);
		this.properties = properties;
		logger.info("servletPath:{}", servletPath);
	}
	
	@Override
	public void refresh() {
		doRefresh();
	}
	
	@Override
	protected Map<String, ZuulRoute> locateRoutes() {
		LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<String, ZuulRoute>();
		// 从application.properties中加载路由信息
		// routesMap.putAll(super.locateRoutes());
		// 从db中加载路由信息
		routesMap.putAll(loadLocateRoute());
		// 优化一下配置
		LinkedHashMap<String, ZuulRoute> values = new LinkedHashMap<String, ZuulRoute>();
		for (Map.Entry<String, ZuulRoute> entry : routesMap.entrySet()) {
			String path = entry.getKey();
			// Prepend with slash if not already present.
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (StringUtils.hasText(this.properties.getPrefix())) {
				path = this.properties.getPrefix() + path;
				if (!path.startsWith("/")) {
					path = "/" + path;
				}
			}
			values.put(path, entry.getValue());
		}
		return values;
	}
	
	/**
	 * @description 加载路由配置，由子类去实现
	 * @version 1.0.0
	 * @return
	 */
	public abstract Map<String, ZuulRoute> loadLocateRoute();
	
	/**
	 * @description 获取路由规则，由子类去实现
	 * @version 1.0.0
	 * @param route
	 * @return
	 */
	public abstract List<IZuulRouteRule> getRules(Route route);
	
	@Override
	public Route getMatchingRoute(String path) {
		Route route = super.getMatchingRoute(path);
		// 增加自定义路由规则判断
		List<IZuulRouteRule> rules = getRules(route);
		return zuulRouteRuleMatcher.matchingRule(route, rules);
	}
	
	@Override
	public int getOrder() {
		return -1;
	}
	
	/**
	 * @description 复制查询出来的数据的属性
	 * @version 1.0.0
	 * @param locateRouteList
	 * @return
	 */
	public Map<String, ZuulRoute> handle(List<ZuulRouteEntity> locateRouteList){
		if(CollectionUtils.isEmpty(locateRouteList)){
			return null;
		}
		Map<String, ZuulRoute> routes = new LinkedHashMap<String, ZuulRoute>();
		for (ZuulRouteEntity locateRoute : locateRouteList) {
			if (StringUtils.isEmpty(locateRoute.getPath())|| !locateRoute.isEnable()|| (StringUtils.isEmpty(locateRoute.getUrl()) && StringUtils.isEmpty(locateRoute.getServiceId()))) {
				continue;
			}
			ZuulRoute zuulRoute = new ZuulRoute();
			try {
				zuulRoute.setCustomSensitiveHeaders(locateRoute.isCustomSensitiveHeaders());
				zuulRoute.setSensitiveHeaders(locateRoute.getSensitiveHeadersSet());
				zuulRoute.setId(locateRoute.getId());
				//zuulRoute.setLocation("");
				zuulRoute.setPath(locateRoute.getPath());
				zuulRoute.setRetryable(locateRoute.isRetryable());
				zuulRoute.setServiceId(locateRoute.getServiceId());
				zuulRoute.setStripPrefix(locateRoute.isStripPrefix());
				zuulRoute.setUrl(locateRoute.getUrl());
				logger.info("add maven.com.hhf.alaska.spring-boot-starter-zuul route: " + JsonUtils.toJson(zuulRoute));
			} catch (Exception e) {
				logger.error("load maven.com.hhf.alaska.spring-boot-starter-zuul route info from db with error", e);
			}
			routes.put(zuulRoute.getPath(), zuulRoute);
		}
		return routes;
	}
}
