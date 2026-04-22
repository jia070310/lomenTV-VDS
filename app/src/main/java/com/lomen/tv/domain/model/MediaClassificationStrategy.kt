package com.lomen.tv.domain.model

/**
 * 媒体类型识别策略（用于刮削时区分电影/电视剧/综艺等）
 *
 * - [SMART_BALANCED]：综合季集结构与类型关键词做平衡判断，优先减少误判（推荐）。
 * - [STRUCTURE_FIRST]：优先根据文件名中的季集结构（SxxEyy、纯集数等）判断，再参考关键词。
 * - [KEYWORD_FIRST]：优先根据路径/文件名中的类型关键词（综艺、纪录片、演唱会等）判断，再参考季集结构。
 */
enum class MediaClassificationStrategy {
    SMART_BALANCED,
    STRUCTURE_FIRST,
    KEYWORD_FIRST
}
