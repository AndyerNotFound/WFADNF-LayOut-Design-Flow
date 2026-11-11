package com.andyer.layoutflow.model

data class PlatformInfo(
    val id: String,
    val label: String,
    val short: String,
    val color: Long,
)

data class ComponentSpec(
    val id: String,
    val name: String,
    val category: String,
    val platform: String,
    val kind: String,
    val width: Float,
    val height: Float,
    val text: String = "",
    val description: String = "",
)

data class TriggerSpec(val id: String, val label: String, val short: String)
data class ActionSpec(val id: String, val label: String, val short: String)
data class TransitionSpec(val id: String, val label: String, val short: String, val hint: String)
data class EasingSpec(val id: String, val label: String)

val PLATFORMS = listOf(
    PlatformInfo("android", "Android", "A", 0xFF6750A4),
    PlatformInfo("web", "Web", "W", 0xFF0061A4),
    PlatformInfo("flutter", "Flutter", "F", 0xFF006A6A),
    PlatformInfo("ios", "iOS", "i", 0xFF4F5B66),
    PlatformInfo("desktop", "桌面", "D", 0xFF8A5100),
    PlatformInfo("logic", "逻辑", "L", 0xFF00696D),
    PlatformInfo("custom", "自定义", "C", 0xFF984061),
)

fun platformInfo(id: String): PlatformInfo =
    PLATFORMS.firstOrNull { it.id == id } ?: PLATFORMS.last()

private fun c(
    platform: String,
    key: String,
    name: String,
    category: String,
    kind: String,
    width: Float,
    height: Float,
    text: String = "",
    description: String = "",
): ComponentSpec = ComponentSpec(
    id = "$platform.$key",
    name = name,
    category = category,
    platform = platform,
    kind = kind,
    width = width,
    height = height,
    text = text,
    description = description,
)

val COMPONENTS: List<ComponentSpec> = listOf(
    c("android", "screen", "页面 / Activity", "视图", "screen", 280f, 500f, "页面", "承载 Compose、View、Activity 或 Fragment 的视图容器"),
    c("android", "dialog", "对话框", "视图", "dialog", 240f, 180f, "提示", "Material 3 基础对话框"),
    c("android", "sheet", "底部面板", "视图", "sheet", 280f, 200f, "底部面板", "ModalBottomSheet 或 BottomSheetDialog"),
    c("android", "topappbar", "顶部应用栏", "导航", "topbar", 280f, 64f, "标题", "TopAppBar / CenterAlignedTopAppBar"),
    c("android", "bottomappbar", "底部应用栏", "导航", "bottomappbar", 280f, 72f, "", "BottomAppBar"),
    c("android", "navigationbar", "导航栏", "导航", "bottomnav", 280f, 80f, "", "NavigationBar，三到五个目的地"),
    c("android", "navrail", "导航轨", "导航", "navrail", 96f, 300f, "", "NavigationRail，适配大屏"),
    c("android", "tabs", "标签页", "导航", "tabs", 260f, 48f, "标签", "TabRow / PrimaryTabRow"),
    c("android", "drawer", "导航抽屉", "导航", "menu", 220f, 240f, "菜单", "ModalNavigationDrawer"),
    c("android", "button", "填充按钮", "按钮", "button", 120f, 48f, "按钮", "Button，最高优先级操作"),
    c("android", "tonalbutton", "色调按钮", "按钮", "buttonTonal", 120f, 48f, "按钮", "FilledTonalButton，中优先级操作"),
    c("android", "outlinedbutton", "描边按钮", "按钮", "buttonOutlined", 120f, 48f, "按钮", "OutlinedButton，次优先级操作"),
    c("android", "textbutton", "文本按钮", "按钮", "buttonText", 100f, 40f, "按钮", "TextButton，最低优先级操作"),
    c("android", "fab", "浮动操作按钮", "按钮", "fab", 56f, 56f, "+", "FloatingActionButton"),
    c("android", "extendedfab", "扩展浮动按钮", "按钮", "extFab", 150f, 56f, "新建", "ExtendedFloatingActionButton"),
    c("android", "iconbutton", "图标按钮", "按钮", "iconButton", 48f, 48f, "★", "IconButton"),
    c("android", "textfield", "填充输入框", "输入", "textfield", 220f, 56f, "输入内容", "TextField"),
    c("android", "outlinedtextfield", "描边输入框", "输入", "textfieldOutlined", 220f, 56f, "输入内容", "OutlinedTextField"),
    c("android", "search", "搜索栏", "输入", "search", 240f, 56f, "搜索", "SearchBar / SearchView"),
    c("android", "switch", "开关", "选择", "switch", 64f, 32f, "", "Switch"),
    c("android", "checkbox", "复选框", "选择", "checkbox", 48f, 48f, "", "Checkbox"),
    c("android", "radio", "单选按钮", "选择", "radio", 48f, 48f, "", "RadioButton"),
    c("android", "slider", "滑块", "选择", "slider", 200f, 40f, "", "Slider / RangeSlider"),
    c("android", "chip", "标签片", "选择", "chip", 96f, 32f, "标签", "FilterChip / AssistChip"),
    c("android", "card", "卡片", "容器", "card", 220f, 140f, "卡片标题", "Card / ElevatedCard"),
    c("android", "list", "列表", "容器", "list", 220f, 160f, "列表", "LazyColumn / RecyclerView"),
    c("android", "image", "图片", "展示", "image", 180f, 120f, "", "Image / ImageView"),
    c("android", "avatar", "头像", "展示", "avatar", 56f, 56f, "", "圆形头像"),
    c("android", "icon", "图标", "展示", "icon", 48f, 48f, "★", "Icon / ImageVector"),
    c("android", "badge", "徽标", "展示", "badge", 64f, 28f, "99+", "Badge"),
    c("android", "chart", "图表", "展示", "chart", 220f, 140f, "", "Canvas 或图表库"),
    c("android", "map", "地图", "展示", "map", 220f, 160f, "", "Maps Compose / MapView"),
    c("android", "video", "视频", "展示", "video", 220f, 140f, "", "ExoPlayer / VideoView"),
    c("android", "webview", "网页视图", "展示", "web", 240f, 180f, "WebView", "WebView 或 Custom Tab"),
    c("android", "snackbar", "消息条", "反馈", "snackbar", 240f, 56f, "消息", "Snackbar"),
    c("android", "progress", "进度", "反馈", "progress", 160f, 16f, "", "LinearProgressIndicator"),
    c("android", "datepicker", "日期选择", "反馈", "calendar", 240f, 260f, "日期", "DatePicker"),
    c("android", "timepicker", "时间选择", "反馈", "calendar", 240f, 220f, "时间", "TimePicker"),
    c("android", "dropdown", "下拉菜单", "导航", "menu", 180f, 160f, "菜单", "ExposedDropdownMenu / Menu"),
    c("android", "recyclerview", "滚动视图", "容器", "list", 220f, 180f, "列表", "RecyclerView / LazyList"),

    c("web", "page", "网页", "视图", "screen", 300f, 500f, "网页", "浏览器页面或路由视图"),
    c("web", "header", "页头", "导航", "topbar", 280f, 64f, "站点标题", "header / nav"),
    c("web", "nav", "导航", "导航", "tabs", 280f, 48f, "首页", "nav / 菜单"),
    c("web", "button", "按钮", "按钮", "button", 110f, 40f, "按钮", "button 元素"),
    c("web", "link", "链接", "导航", "link", 100f, 32f, "链接", "a 元素"),
    c("web", "input", "输入框", "输入", "textfieldOutlined", 200f, 44f, "输入内容", "input 元素"),
    c("web", "textarea", "多行输入", "输入", "textarea", 220f, 100f, "多行内容", "textarea 元素"),
    c("web", "select", "下拉选择", "选择", "menu", 180f, 44f, "请选择", "select 元素"),
    c("web", "checkbox", "复选框", "选择", "checkbox", 44f, 44f, "", "input[type=checkbox]"),
    c("web", "radio", "单选框", "选择", "radio", 44f, 44f, "", "input[type=radio]"),
    c("web", "range", "范围滑块", "选择", "slider", 200f, 36f, "", "input[type=range]"),
    c("web", "card", "卡片", "容器", "card", 220f, 140f, "卡片标题", "article / 卡片组件"),
    c("web", "modal", "模态框", "视图", "dialog", 240f, 180f, "对话框", "dialog / modal"),
    c("web", "dropdown", "下拉菜单", "导航", "menu", 180f, 160f, "菜单", "details / popover"),
    c("web", "toast", "轻提示", "反馈", "snackbar", 220f, 52f, "提示", "toast / 通知"),
    c("web", "image", "图片", "展示", "image", 180f, 120f, "", "img 元素"),
    c("web", "video", "视频", "展示", "video", 220f, 140f, "", "video 元素"),
    c("web", "table", "表格", "展示", "table", 240f, 160f, "", "table 元素"),
    c("web", "form", "表单", "容器", "card", 240f, 220f, "表单", "form 元素"),
    c("web", "tabs", "选项卡", "导航", "tabs", 240f, 44f, "选项", "tablist"),
    c("web", "sidebar", "侧边栏", "导航", "navrail", 96f, 300f, "", "aside / 侧栏"),
    c("web", "footer", "页脚", "导航", "bottomappbar", 280f, 64f, "页脚", "footer"),
    c("web", "canvas", "画布", "展示", "custom", 220f, 160f, "Canvas", "canvas 元素"),

    c("flutter", "scaffold", "Scaffold", "视图", "screen", 280f, 500f, "页面", "Flutter 页面骨架"),
    c("flutter", "appbar", "AppBar", "导航", "topbar", 280f, 64f, "标题", "AppBar"),
    c("flutter", "fab", "FloatingActionButton", "按钮", "fab", 56f, 56f, "+", "FloatingActionButton"),
    c("flutter", "elevatedbutton", "ElevatedButton", "按钮", "button", 120f, 48f, "按钮", "ElevatedButton"),
    c("flutter", "textfield", "TextField", "输入", "textfield", 220f, 56f, "输入内容", "TextField"),
    c("flutter", "switch", "Switch", "选择", "switch", 64f, 32f, "", "Switch"),
    c("flutter", "checkbox", "Checkbox", "选择", "checkbox", 48f, 48f, "", "Checkbox"),
    c("flutter", "card", "Card", "容器", "card", 220f, 140f, "卡片标题", "Card"),
    c("flutter", "listview", "ListView", "容器", "list", 220f, 160f, "列表", "ListView / CustomScrollView"),
    c("flutter", "bottomnav", "BottomNavigationBar", "导航", "bottomnav", 280f, 80f, "", "BottomNavigationBar"),
    c("flutter", "drawer", "Drawer", "导航", "menu", 220f, 240f, "菜单", "Drawer"),
    c("flutter", "dialog", "AlertDialog", "视图", "dialog", 240f, 180f, "提示", "AlertDialog / Dialog"),
    c("flutter", "snackbar", "SnackBar", "反馈", "snackbar", 240f, 56f, "消息", "SnackBar"),
    c("flutter", "tabbar", "TabBar", "导航", "tabs", 260f, 48f, "标签", "TabBar / TabBarView"),
    c("flutter", "image", "Image", "展示", "image", 180f, 120f, "", "Image widget"),
    c("flutter", "pageview", "PageView", "视图", "screen", 280f, 500f, "轮播页", "PageView"),

    c("ios", "view", "View", "视图", "screen", 280f, 500f, "视图", "SwiftUI View / UIViewController"),
    c("ios", "navigationstack", "NavigationStack", "导航", "screen", 280f, 500f, "导航页", "NavigationStack / UINavigationController"),
    c("ios", "tabview", "TabView", "导航", "bottomnav", 280f, 80f, "", "TabView / UITabBarController"),
    c("ios", "button", "Button", "按钮", "button", 120f, 44f, "按钮", "UIButton / SwiftUI Button"),
    c("ios", "textfield", "TextField", "输入", "textfield", 220f, 52f, "输入内容", "UITextField / TextField"),
    c("ios", "toggle", "Toggle", "选择", "switch", 64f, 32f, "", "UISwitch / SwiftUI Toggle"),
    c("ios", "picker", "Picker", "选择", "menu", 180f, 44f, "请选择", "UIPickerView / Picker"),
    c("ios", "list", "List", "容器", "list", 220f, 160f, "列表", "UITableView / List"),
    c("ios", "card", "GroupBox", "容器", "card", 220f, 140f, "卡片标题", "GroupBox / 自定义卡片"),
    c("ios", "sheet", "Sheet", "视图", "sheet", 280f, 200f, "面板", "sheet / UISheetPresentationController"),
    c("ios", "alert", "Alert", "视图", "dialog", 240f, 180f, "提示", "UIAlertController / Alert"),
    c("ios", "image", "Image", "展示", "image", 180f, 120f, "", "UIImageView / Image"),
    c("ios", "tabbar", "TabBar", "导航", "tabs", 260f, 48f, "标签", "UITabBar / 自定义标签栏"),

    c("desktop", "window", "窗口", "视图", "screen", 320f, 480f, "窗口", "Windows / macOS / Linux 窗口"),
    c("desktop", "menubar", "菜单栏", "导航", "menu", 220f, 160f, "菜单", "菜单栏 / Command Menu"),
    c("desktop", "toolbar", "工具栏", "导航", "tabs", 280f, 48f, "工具", "Toolbar / Action Bar"),
    c("desktop", "button", "按钮", "按钮", "button", 120f, 40f, "按钮", "桌面按钮"),
    c("desktop", "textfield", "输入框", "输入", "textfieldOutlined", 220f, 44f, "输入内容", "文本框"),
    c("desktop", "list", "列表", "容器", "list", 220f, 160f, "列表", "列表 / 表格视图"),
    c("desktop", "dialog", "对话框", "视图", "dialog", 240f, 180f, "提示", "模态对话框"),
    c("desktop", "tray", "托盘菜单", "导航", "badge", 80f, 28f, "托盘", "系统托盘 / 状态栏菜单"),
    c("desktop", "canvas", "绘图画布", "展示", "custom", 240f, 180f, "Canvas", "自绘画布"),

    c("logic", "start", "起点", "逻辑", "start", 120f, 52f, "开始", "流程入口"),
    c("logic", "condition", "条件分支", "逻辑", "condition", 160f, 90f, "条件?", "判断与分支"),
    c("logic", "state", "状态", "逻辑", "state", 160f, 80f, "状态", "界面状态或数据状态"),
    c("logic", "api", "接口请求", "逻辑", "api", 160f, 80f, "请求", "网络请求与返回"),
    c("logic", "timer", "定时器", "逻辑", "timer", 140f, 72f, "1s", "延时与轮询"),
    c("logic", "note", "注释", "逻辑", "note", 180f, 100f, "说明", "流程说明"),
    c("logic", "group", "分组", "逻辑", "group", 260f, 180f, "分组", "视觉分组容器"),
    c("logic", "end", "终点", "逻辑", "end", 120f, 52f, "结束", "流程出口"),

    c("custom", "custom", "自定义元件", "自定义", "custom", 180f, 120f, "自定义", "任意平台、组件或交互的占位节点"),
)

val COMPONENT_MAP: Map<String, ComponentSpec> = COMPONENTS.associateBy { it.id }

fun componentSpec(id: String): ComponentSpec =
    COMPONENT_MAP[id] ?: COMPONENTS.last().copy(id = id)

fun specForNode(node: FlowNode): ComponentSpec =
    COMPONENT_MAP[node.specId] ?: ComponentSpec(
        id = node.specId,
        name = node.name,
        category = "自定义",
        platform = node.platform,
        kind = node.kind,
        width = node.width,
        height = node.height,
        text = node.text,
        description = "",
    )

val CATEGORIES = COMPONENTS.map { it.category }.distinct()

val QUICK_SPECS: List<ComponentSpec> = listOf(
    "android.screen",
    "android.button",
    "android.textfield",
    "android.card",
    "android.switch",
    "android.navigationbar",
    "web.button",
    "logic.condition",
    "custom.custom",
).mapNotNull { COMPONENT_MAP[it] }

val TRIGGERS = listOf(
    TriggerSpec("click", "点击", "点击"),
    TriggerSpec("longClick", "长按", "长按"),
    TriggerSpec("doubleTap", "双击", "双击"),
    TriggerSpec("swipeLeft", "左滑", "左滑"),
    TriggerSpec("swipeRight", "右滑", "右滑"),
    TriggerSpec("swipeUp", "上滑", "上滑"),
    TriggerSpec("swipeDown", "下滑", "下滑"),
    TriggerSpec("drag", "拖拽", "拖拽"),
    TriggerSpec("pinch", "双指缩放", "缩放"),
    TriggerSpec("scroll", "滚动", "滚动"),
    TriggerSpec("focus", "获得焦点", "聚焦"),
    TriggerSpec("hover", "悬停", "悬停"),
    TriggerSpec("key", "按键", "按键"),
    TriggerSpec("back", "返回", "返回"),
    TriggerSpec("load", "加载完成", "加载"),
    TriggerSpec("timer", "定时触发", "定时"),
    TriggerSpec("valueChange", "值变化", "值变化"),
    TriggerSpec("notification", "通知点击", "通知"),
    TriggerSpec("voice", "语音指令", "语音"),
    TriggerSpec("shake", "摇一摇", "摇动"),
    TriggerSpec("deepLink", "深链接", "深链"),
    TriggerSpec("custom", "自定义触发", "触发"),
)

val ACTIONS = listOf(
    ActionSpec("navigate", "跳转视图", "跳转"),
    ActionSpec("openDialog", "打开对话框", "弹窗"),
    ActionSpec("openSheet", "打开底部面板", "面板"),
    ActionSpec("dismiss", "关闭当前视图", "关闭"),
    ActionSpec("back", "返回上一级", "返回"),
    ActionSpec("scrollTo", "滚动到位置", "滚动"),
    ActionSpec("updateState", "更新状态", "更新"),
    ActionSpec("openUrl", "打开链接", "链接"),
    ActionSpec("share", "分享", "分享"),
    ActionSpec("requestPermission", "请求权限", "权限"),
    ActionSpec("startActivity", "启动 Activity", "启动"),
    ActionSpec("finish", "结束页面", "结束"),
    ActionSpec("showSnackbar", "显示提示", "提示"),
    ActionSpec("focus", "聚焦元件", "聚焦"),
    ActionSpec("callApi", "调用接口", "接口"),
    ActionSpec("custom", "自定义动作", "动作"),
)

val TRANSITIONS = listOf(
    TransitionSpec("none", "无动画", "无", "瞬切"),
    TransitionSpec("fade", "淡入淡出", "淡入", "Fade"),
    TransitionSpec("fadeThrough", "穿透淡入", "穿透", "Material fade through"),
    TransitionSpec("slideLeft", "向左滑入", "左滑入", "Slide left"),
    TransitionSpec("slideRight", "向右滑入", "右滑入", "Slide right"),
    TransitionSpec("slideUp", "向上滑入", "上滑入", "Slide up"),
    TransitionSpec("slideDown", "向下滑入", "下滑入", "Slide down"),
    TransitionSpec("sharedAxisX", "共享轴 X", "共享轴X", "Shared axis X"),
    TransitionSpec("sharedAxisY", "共享轴 Y", "共享轴Y", "Shared axis Y"),
    TransitionSpec("sharedAxisZ", "共享轴 Z", "共享轴Z", "Shared axis Z"),
    TransitionSpec("containerTransform", "容器变换", "容器变换", "Container transform"),
    TransitionSpec("scale", "缩放", "缩放", "Scale"),
    TransitionSpec("reveal", "揭示", "揭示", "Reveal"),
    TransitionSpec("custom", "自定义动画", "自定义", "Custom"),
)

val EASINGS = listOf(
    EasingSpec("standard", "标准曲线"),
    EasingSpec("emphasized", "强调曲线"),
    EasingSpec("emphasizedDecelerate", "强调减速"),
    EasingSpec("emphasizedAccelerate", "强调加速"),
    EasingSpec("linear", "线性"),
    EasingSpec("overshoot", "回弹"),
)

fun triggerSpec(id: String): TriggerSpec = TRIGGERS.firstOrNull { it.id == id } ?: TRIGGERS.last()
fun actionSpec(id: String): ActionSpec = ACTIONS.firstOrNull { it.id == id } ?: ACTIONS.last()
fun transitionSpec(id: String): TransitionSpec = TRANSITIONS.firstOrNull { it.id == id } ?: TRANSITIONS.first()
fun easingSpec(id: String): EasingSpec = EASINGS.firstOrNull { it.id == id } ?: EASINGS.first()

fun FlowEdge.autoLabel(): String {
    val fromLabel = if (condition.isBlank()) "" else "${condition.trim()} · "
    return "$fromLabel${triggerSpec(trigger).short} · ${actionSpec(action).short} · ${transitionSpec(transition).short} · ${duration}ms"
}

fun FlowEdge.displayLabel(): String = label.ifBlank { autoLabel() }
