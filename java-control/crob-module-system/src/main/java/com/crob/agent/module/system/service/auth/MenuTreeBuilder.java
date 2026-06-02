package com.crob.agent.module.system.service.auth;

import com.crob.agent.module.system.dal.dataobject.SysMenuDO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MenuTreeBuilder {

    public List<MenuNode> buildTree(List<SysMenuDO> flatMenus) {
        if (flatMenus == null || flatMenus.isEmpty()) {
            return new ArrayList<>();
        }
        List<MenuNode> nodes = flatMenus.stream()
                .map(MenuNode::new)
                .sorted(Comparator.comparingInt(n -> n.sort != null ? n.sort : 0))
                .collect(Collectors.toList());
        Map<Long, List<MenuNode>> parentIdMap = nodes.stream()
                .filter(n -> n.parentId != null && n.parentId > 0)
                .collect(Collectors.groupingBy(n -> n.parentId));
        List<MenuNode> roots = new ArrayList<>();
        for (MenuNode node : nodes) {
            List<MenuNode> children = parentIdMap.get(node.id);
            if (children != null) {
                node.children = children.stream()
                        .sorted(Comparator.comparingInt(n -> n.sort != null ? n.sort : 0))
                        .collect(Collectors.toList());
            }
            if (node.parentId == null || node.parentId == 0) {
                roots.add(node);
            }
        }
        return roots;
    }

    public static class MenuNode {
        public Long id;
        public String name;
        public String permission;
        public Integer type;
        public Integer sort;
        public Long parentId;
        public String path;
        public String icon;
        public String component;
        public String componentName;
        public Boolean visible;
        public Boolean keepAlive;
        public Boolean alwaysShow;
        public String redirect;
        public List<MenuNode> children;

        public MenuNode(SysMenuDO m) {
            this.id = m.getId();
            this.name = m.getName();
            this.permission = m.getPermission();
            this.type = m.getType();
            this.sort = m.getSort();
            this.parentId = m.getParentId();
            this.path = m.getPath();
            this.icon = m.getIcon();
            this.component = m.getComponent();
            this.componentName = m.getComponentName();
            this.visible = m.getVisible();
            this.keepAlive = m.getKeepAlive();
            this.alwaysShow = m.getAlwaysShow();
            this.children = new ArrayList<>();
        }
    }
}
