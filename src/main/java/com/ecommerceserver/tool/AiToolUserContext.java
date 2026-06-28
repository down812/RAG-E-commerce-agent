package com.ecommerceserver.tool;

import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工具调用的用户上下文持有器。
 *
 * <p>背景：AI 工具（如 {@link CartTool#addToCart}）在 reactor 线程上执行，拿不到请求线程的
 * ThreadLocal（LoginContext），而 Spring AI 1.0.0-M6 的 ToolContext 机制要求“被调用的所有工具
 * 都声明 ToolContext 参数”，否则抛 {@code ToolContext is not supported by the method as an argument}，
 * 且 ToolContext 会被渲染进工具 inputSchema、污染工具定义。
 *
 * <p>因此改用静态持有器：在 {@code ChatServiceImpl.analyse} 的请求线程中把 userId 按 sessionId
 * 绑定进静态快照；工具在 reactor 线程执行时通过该快照跨线程读取，流结束（doFinally）时清理。
 *
 * <p>读取优先级：① 同线程 {@link ThreadLocal}（若恰好同线程）；② 当前仅有一个活跃会话时取其 userId；
 * ③ 最近一次绑定值兜底。高并发多会话且工具线程无法定位会话的极端场景下，②/③ 存在理论串号可能，
 * 但本场景下每个 sessionId 同一时刻仅一个在途请求，足够可靠。
 */
public final class AiToolUserContext {

    private AiToolUserContext() {
    }

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    /** sessionId -> userId 的兜底快照，用于 ThreadLocal 因线程切换丢失时按需恢复。 */
    private static final ConcurrentHashMap<String, Long> SESSION_USER = new ConcurrentHashMap<>();

    /** 最近一次绑定的 userId（单活跃请求场景的最终兜底）。 */
    private static volatile Long LAST_BOUND_USER;

    /** 绑定当前线程与会话的 userId（在每个流信号处理时调用）。 */
    public static void bind(String sessionId, Long userId) {
        if (userId == null) {
            return;
        }
        CURRENT_USER.set(userId);
        LAST_BOUND_USER = userId;
        if (sessionId != null) {
            SESSION_USER.put(sessionId, userId);
        }
    }

    /** 流结束时清理，避免线程池复用导致串号与内存泄漏。 */
    public static void clear(String sessionId) {
        CURRENT_USER.remove();
        if (sessionId != null) {
            SESSION_USER.remove(sessionId);
        }
    }

    /** 工具方法读取当前 userId：优先同线程 ThreadLocal，其次最近绑定值兜底。 */
    public static Long currentUserId() {
        Long uid = CURRENT_USER.get();
        if (uid != null) {
            return uid;
        }
        // 跨线程兜底：当前仅有一个活跃会话时取其 userId，否则取最近绑定值
        if (SESSION_USER.size() == 1) {
            return SESSION_USER.values().iterator().next();
        }
        return LAST_BOUND_USER;
    }
}
