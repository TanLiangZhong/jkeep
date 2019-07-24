package com.ml.jkeep.service.system.impl;

import com.ml.jkeep.common.constant.CacheKey;
import com.ml.jkeep.jpa.system.entity.sys.*;
import com.ml.jkeep.jpa.system.repository.*;
import com.ml.jkeep.jpa.system.vo.HrefPermissionVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 鉴权 -impl
 *
 * @author 谭良忠
 * @date 2019/6/20 10:58
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = CacheKey.HREF_PERMISSION_NAME)
public class AuthServiceImpl implements UserDetailsService {

    private final UserRoleRepository userRoleRepository;
    private final RoleLinkRepository roleLinkRepository;
    private final ElementRepository elementRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("loadUserByUsername, username: {}", username);
        User user = userRepository.findByUsername(username);
        if (user == null) {
            log.error("用户名不存在, {}", username);
            throw new UsernameNotFoundException("请输入正确的用户名");
        }
        UserAuth userAuth = new UserAuth();
        userAuth.setUsername(user.getUsername());
        userAuth.setPassword(user.getPassword());
        userAuth.setHrefPer(this.hrefPermission(user.getUserId()));
        return userAuth;
    }

    /**
     * 获取用户可访问的 href
     *
     * @param userId 用户Id
     * @return 用户所有权限
     */
    @Cacheable(key = "T(com.ml.jkeep.common.constant.CacheKey).ALL_HREF_PERMISSION_KEY+#userId")
    public Set<HrefPermissionVo> hrefPermission(Long userId) {
        log.info("测试缓存, userId: {} ", userId);
        List<RoleLink> roleLinkList = roleLinkRepository.findAllByRoleIdIn(Optional.ofNullable(userRoleRepository.findAllByUserId(userId)).orElse(new ArrayList<>()).stream().map(UserRole::getRoleId).collect(Collectors.toSet()));
        return this.convert(roleLinkList);
    }

    /**
     * 获取所有href权限配置
     *
     * @return 所有权限
     */
    @Cacheable(key = "T(com.ml.jkeep.common.constant.CacheKey).ALL_HREF_PERMISSION_KEY")
    public Set<HrefPermissionVo> hrefPermission() {
        log.info("测试缓存, ALL_HREF_PERMISSION_KEY");
        List<RoleLink> roleLinkList = roleLinkRepository.findAll();
        return this.convert(roleLinkList);
    }

    private Set<HrefPermissionVo> convert(List<RoleLink> roleLinkList) {
        if (CollectionUtils.isEmpty(roleLinkList)) {
            return new HashSet<>(0);
        }
        Set<Long> menuIds = new HashSet<>();
        Set<Long> elementIds = new HashSet<>();
        roleLinkList.forEach(roleLink -> {
            menuIds.add(roleLink.getMenuId());
            elementIds.add(roleLink.getElementId());
        });
        List<Menu> menuList = menuRepository.findAllByMenuIdIn(menuIds);
        List<Element> elementList = elementRepository.findAllByElementIdIn(elementIds);
        Set<HrefPermissionVo> vos = new HashSet<>();
        menuList.forEach(menu -> vos.add(new HrefPermissionVo(menu.getCode(), menu.getHref())));
        elementList.forEach(menu -> vos.add(new HrefPermissionVo(menu.getCode(), menu.getHref())));
        return vos;
    }
}
