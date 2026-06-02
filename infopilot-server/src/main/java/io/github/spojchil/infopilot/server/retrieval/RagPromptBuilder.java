package io.github.spojchil.infopilot.server.retrieval;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.List;
import org.springframework.stereotype.Component;

/** RAG 提示词构建器。将检索结果拼接为带来源标注的 LLM 上下文。 */
@Component
public class RagPromptBuilder {

    private static final String RAG_PROMPT =
            """
            你是 InfoPilot，一个企业文档智能助手。
            基于以下参考文档片段回答用户问题。

            规则：
            - 如果文档中有相关信息，请引用具体内容并标注来源
            - 如果文档中没有相关信息，直接说"未在文档中找到相关信息"
            - 不要编造文档中没有的内容
            - 回答应简洁、准确""";

    /**
     * 组装完整的 RAG 提示词。
     *
     * @param query 用户问题
     * @param matches 检索到的文档片段
     * @return System Message 用的增强提示词
     */
    public String buildSystemPrompt(String query, List<EmbeddingMatch<TextSegment>> matches) {
        StringBuilder sb = new StringBuilder(RAG_PROMPT);
        sb.append("\n\n## 参考文档\n\n");
        for (var m : matches) {
            String fileName =
                    m.embedded().metadata().getString("fileName") != null
                            ? m.embedded().metadata().getString("fileName")
                            : "未知文档";
            sb.append("[来源: ")
                    .append(fileName)
                    .append(", 相似度: ")
                    .append(String.format("%.2f", m.score()))
                    .append("]\n");
            sb.append(m.embedded().text()).append("\n\n---\n\n");
        }
        sb.append("\n## 用户问题\n").append(query);
        return sb.toString();
    }
}
