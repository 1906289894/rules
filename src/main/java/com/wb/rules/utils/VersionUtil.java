package com.wb.rules.utils;

import com.wb.rules.common.exceptions.RuleException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义化版本号工具类 (遵循 Semantic Versioning 2.0.0 规范)
 * 支持版本号的解析、比较、递增和验证。
 */
@Getter
@Slf4j
public class VersionUtil implements Comparable<VersionUtil> {

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease; // 预发布标识，如 "alpha", "beta.1"
    private final String buildMetadata; // 构建元数据，如 "build.001"

    // 正则表达式用于解析语义化版本号字符串
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^v?" + // 可选的 'v' 前缀
                    "(\\d+)\\.(\\d+)\\.(\\d+)" + // 主版本号.次版本号.修订号 (必需)
                    "(?:-([a-zA-Z0-9-.]+))?" + // 可选的预发布标识
                    "(?:\\+([a-zA-Z0-9-.]+))?$" // 可选的构建元数据
    );

    /**
     * 通过解析版本字符串构造 VersionUtil 对象。
     *
     * @param version 版本字符串，例如 "1.2.3", "v2.0.1-alpha+build.1"
     * @throws IllegalArgumentException 如果版本字符串格式无效
     */
    public VersionUtil(String version) {
        Matcher matcher = SEMVER_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("无效的语义化版本号格式: " + version);
        }

        try {
            this.major = Integer.parseInt(matcher.group(1));
            this.minor = Integer.parseInt(matcher.group(2));
            this.patch = Integer.parseInt(matcher.group(3));
            // group(4) 是预发布标识，group(5) 是构建元数据，可能为null
            this.preRelease = matcher.group(4);
            this.buildMetadata = matcher.group(5);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("版本号数字部分解析错误: " + version, e);
        }

        // 验证数字部分非负
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("版本号数字部分不能为负数: " + version);
        }
    }

    /**
     * 通过数字部分构造 VersionUtil 对象 (预发布和构建元数据为空)。
     */
    public VersionUtil(int major, int minor, int patch) {
        this(major, minor, patch, null, null);
    }

    /**
     * 完整构造函数。
     */
    public VersionUtil(int major, int minor, int patch, String preRelease, String buildMetadata) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.buildMetadata = buildMetadata;
    }

    // Getters
    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }
    public String getPreRelease() { return preRelease; }
    public String getBuildMetadata() { return buildMetadata; }

    /**
     * 检查版本号字符串是否有效。
     */
    public static boolean isValid(String version) {
        return version != null && SEMVER_PATTERN.matcher(version.trim()).matches();
    }

    /**
     * 核心比较逻辑：比较当前版本与另一个版本。
     * 遵循语义化版本规范：先比较数字部分，数字相同则有预发布标识的版本优先级较低。
     *
     * @param other 要比较的另一个版本对象
     * @return 如果当前版本大于 other 返回正整数，小于返回负整数，相等返回 0
     */
    @Override
    public int compareTo(VersionUtil other) {
        // 1. 比较主版本号
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;

        // 2. 比较次版本号
        result = Integer.compare(this.minor, other.minor);
        if (result != 0) return result;

        // 3. 比较修订号
        result = Integer.compare(this.patch, other.patch);
        if (result != 0) return result;

        // 4. 数字部分完全相同，比较预发布标识
        // 规范规定：有预发布标识的版本优先级低于没有预发布标识的正式版
        if (this.preRelease == null && other.preRelease == null) {
            return 0; // 都是正式版，相等
        } else if (this.preRelease == null) {
            return 1; // 当前是正式版，优先级高于有预发布标识的 other
        } else if (other.preRelease == null) {
            return -1; // other 是正式版，优先级高于有预发布标识的当前版本
        } else {
            // 两者都有预发布标识，按点号分隔的标识符逐个比较
            return comparePreReleaseIdentifiers(this.preRelease, other.preRelease);
        }
        // 构建元数据不参与优先级比较 (规范 10)
    }

    /**
     * 比较两个预发布标识字符串。
     * 标识符按点号分割，逐个比较：数字按数值大小，非数字按 ASCII 排序顺序。
     */
    private int comparePreReleaseIdentifiers(String pre1, String pre2) {
        String[] identifiers1 = pre1.split("\\.");
        String[] identifiers2 = pre2.split("\\.");
        int minLength = Math.min(identifiers1.length, identifiers2.length);

        for (int i = 0; i < minLength; i++) {
            String id1 = identifiers1[i];
            String id2 = identifiers2[i];

            boolean isNumeric1 = id1.matches("\\d+");
            boolean isNumeric2 = id2.matches("\\d+");

            int compareResult;
            if (isNumeric1 && isNumeric2) {
                // 都是数字标识符，比较数值
                compareResult = Integer.compare(Integer.parseInt(id1), Integer.parseInt(id2));
            } else if (isNumeric1) {
                // 数字标识符的优先级低于非数字标识符
                compareResult = -1;
            } else if (isNumeric2) {
                compareResult = 1;
            } else {
                // 都是非数字标识符，按 ASCII 顺序比较
                compareResult = id1.compareTo(id2);
            }

            if (compareResult != 0) {
                return compareResult;
            }
        }
        // 公共部分都相同，标识符更多的版本优先级高（例如 1.0.0-alpha.1 > 1.0.0-alpha）
        return Integer.compare(identifiers1.length, identifiers2.length);
    }

    // 便捷比较方法
    public boolean isGreaterThan(VersionUtil other) { return this.compareTo(other) > 0; }
    public boolean isLessThan(VersionUtil other) { return this.compareTo(other) < 0; }
    public boolean isEqualTo(VersionUtil other) { return this.compareTo(other) == 0; }

    /**
     * 生成递增后的新版本对象。
     *
     * @param incrementType 递增类型："major", "minor", "patch"
     * @return 递增后的新 VersionUtil 对象
     */
    public VersionUtil increment(String incrementType) {
        switch (incrementType.toLowerCase()) {
            case "major":
                // 主版本号加1，次版本号和修订号归零，预发布标识清除[6](@ref)
                return new VersionUtil(this.major + 1, 0, 0, null, this.buildMetadata);
            case "minor":
                // 次版本号加1，修订号归零，预发布标识清除
                return new VersionUtil(this.major, this.minor + 1, 0, null, this.buildMetadata);
            case "patch":
                // 修订号加1，预发布标识清除
                return new VersionUtil(this.major, this.minor, this.patch + 1, null, this.buildMetadata);
            default:
                throw new IllegalArgumentException("递增类型必须是 'major', 'minor' 或 'patch'");
        }
    }

    /**
     * 设置预发布标识。
     *
     * @param preRelease 新的预发布标识字符串，例如 "beta.1"
     * @return 带有新预发布标识的版本对象
     */
    public VersionUtil withPreRelease(String preRelease) {
        return new VersionUtil(this.major, this.minor, this.patch, preRelease, this.buildMetadata);
    }

    /**
     * 转换为规范的版本字符串表示形式。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (preRelease != null && !preRelease.isEmpty()) {
            sb.append('-').append(preRelease);
        }
        if (buildMetadata != null && !buildMetadata.isEmpty()) {
            sb.append('+').append(buildMetadata);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionUtil that = (VersionUtil) o;
        return major == that.major &&
                minor == that.minor &&
                patch == that.patch &&
                Objects.equals(preRelease, that.preRelease);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease);
    }

    /**
     * 更新规则配置并生成新版本号
     */
    public static String updateRuleWithNewVersion(String currentVersionStr, String changeType) throws Exception {
        // 1. 解析当前版本
        VersionUtil currentVersion = new VersionUtil(currentVersionStr);

        // 2. 根据变更类型决定如何升级版本
        String incrementType;
        if ("BREAKING_CHANGE".equals(changeType)) {
            incrementType = "major"; // 重大不兼容变更
        } else if ("NEW_FEATURE".equals(changeType)) {
            incrementType = "minor"; // 新增功能
        } else {
            incrementType = "patch"; // 问题修复
        }

        // 3. 生成新版本号
        VersionUtil newVersion = currentVersion.increment(incrementType);
        return newVersion.toString();
    }

    public static String updateVersion(String currentVersionStr) {
        try {
            return "v"+updateRuleWithNewVersion(currentVersionStr, "major");
        }catch (Exception e){
            log.error("生成版本号失败：{}", currentVersionStr, e);
            throw new RuleException("生成版本号失败");
        }
    }

    public static String getVersion() {
        return "v1.0.0";
    }
}