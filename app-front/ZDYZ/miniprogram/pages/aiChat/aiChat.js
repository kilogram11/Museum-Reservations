import { chatWithAi } from '../../api/ai';

const EXAMPLE_PROMPTS = [
  '后天下午有票吗',
  '我的预约',
  '可以带背包吗',
  '几点停止入馆'
];

const QUICK_ACTIONS = [
  { label: '查余票', url: '/pages/reserve/reserve' },
  { label: '我的预约', url: '/pages/orderRecord/orderRecord' },
  { label: '参观须知', url: '/pages/noticeReservation/noticeReservation' }
];

let messageSeq = 0;

function nextId(prefix = 'msg') {
  messageSeq += 1;
  return `${prefix}-${Date.now()}-${messageSeq}`;
}

function cloneMessages(messages) {
  return messages.map(message => ({
    ...message,
    blocks: (message.blocks || []).map(block => ({ ...block }))
  }));
}

Page({
  data: {
    messages: [],
    inputValue: '',
    loading: false,
    toView: '',
    isLoggedIn: false,
    examplePrompts: EXAMPLE_PROMPTS,
    quickActions: QUICK_ACTIONS,
    lastUserMessage: ''
  },

  onLoad() {
    this.syncLoginStatus();
    this.resetConversation();
  },

  onShow() {
    this.syncLoginStatus();
  },

  syncLoginStatus() {
    const isLoggedIn = !!wx.getStorageSync('token');
    this.setData({ isLoggedIn });
    return isLoggedIn;
  },

  resetConversation() {
    const welcome = this.createAssistantMessage({
      text: '你好，我是博物馆 AI 助手。可以帮你查余票、看预约记录、取消预约，也能回答参观须知和公告问题。',
      suggestions: EXAMPLE_PROMPTS.slice(0, 3),
      meta: { state: '', stateText: '' }
    });
    this.setData({
      messages: [welcome],
      toView: welcome.id
    });
  },

  onInput(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  sendMessage() {
    const content = (this.data.inputValue || '').trim();
    if (!content || this.data.loading) {
      return;
    }
    this.sendText(content);
  },

  onExampleTap(e) {
    const value = e.currentTarget.dataset.value;
    if (value) {
      this.sendText(value);
    }
  },

  onSuggestionTap(e) {
    const value = e.currentTarget.dataset.value;
    if (!value) {
      return;
    }
    if (value === '重试上一条') {
      this.retryLastMessage();
      return;
    }
    this.sendText(value);
  },

  async sendText(content) {
    if (!content || this.data.loading) {
      return;
    }

    this.syncLoginStatus();
    this.appendMessage(this.createUserMessage(content));
    this.setData({
      inputValue: '',
      loading: true,
      lastUserMessage: content
    });
    this.scrollToBottom();

    try {
      const response = await chatWithAi(content);
      this.appendMessage(this.normalizeAssistantResponse(response));
    } catch (error) {
      this.appendMessage(this.createErrorMessage(error));
    } finally {
      this.setData({ loading: false });
      this.scrollToBottom();
    }
  },

  retryLastMessage() {
    const content = this.data.lastUserMessage;
    if (!content || this.data.loading) {
      return;
    }
    this.sendText(content);
  },

  goLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    });
  },

  goQuickAction(e) {
    const url = e.currentTarget.dataset.url;
    if (!url) {
      return;
    }
    wx.navigateTo({ url });
  },

  toggleBlock(e) {
    const messageId = e.currentTarget.dataset.messageId;
    const blockId = e.currentTarget.dataset.blockId;
    const messages = cloneMessages(this.data.messages);
    const targetMessage = messages.find(message => message.id === messageId);
    if (!targetMessage || !targetMessage.blocks) {
      return;
    }
    const targetBlock = targetMessage.blocks.find(block => block.blockId === blockId);
    if (!targetBlock) {
      return;
    }
    targetBlock.expanded = !targetBlock.expanded;
    this.setData({ messages });
  },

  appendMessage(message) {
    const messages = this.data.messages.concat(message);
    this.setData({
      messages,
      toView: message.id
    });
  },

  createUserMessage(text) {
    return {
      id: nextId('user'),
      role: 'user',
      text,
      blocks: [],
      suggestions: [],
      status: 'success',
      meta: {
        intent: null,
        state: '',
        stateText: '',
        createdAt: Date.now()
      }
    };
  },

  createAssistantMessage({ text, blocks = [], suggestions = [], meta = {}, status = 'success' }) {
    return {
      id: nextId('assistant'),
      role: 'assistant',
      text,
      blocks,
      suggestions,
      status,
      meta: {
        intent: meta.intent || null,
        state: meta.state || '',
        stateText: meta.stateText || '',
        showLoginAction: !!meta.showLoginAction,
        retryable: !!meta.retryable,
        createdAt: Date.now()
      }
    };
  },

  createErrorMessage(error) {
    const text = error && error.message
      ? error.message
      : '网络连接失败，请稍后重试。';
    return this.createAssistantMessage({
      text,
      blocks: [this.createTipBlock('错误提示', text, 'network')],
      suggestions: ['重试上一条'],
      status: 'error',
      meta: {
        state: 'network-error',
        stateText: '网络请求失败，可重试上一条消息。',
        retryable: true
      }
    });
  },

  normalizeAssistantResponse(response) {
    const reply = response && response.reply
      ? response.reply
      : '抱歉，服务暂时不可用。';
    const intent = response && response.intent ? response.intent : null;
    const suggestions = Array.isArray(response && response.suggestions)
      ? response.suggestions.slice(0, 4)
      : [];
    const blocks = this.normalizeBlocks((response && response.blocks) || []);
    const meta = this.inferMeta(reply, intent, blocks);

    return this.createAssistantMessage({
      text: reply,
      blocks,
      suggestions,
      meta: { ...meta, intent }
    });
  },

  normalizeBlocks(blocks) {
    return blocks.map((block, index) => {
      const normalized = {
        type: block.type || 'text',
        title: block.title || '',
        source: block.source || '',
        sourceLabel: this.formatSourceLabel(block.source || ''),
        items: this.normalizeBlockItems(block.type, block.items || []),
        expanded: block.type !== 'rules_source',
        blockId: nextId(`block-${index}`)
      };
      return normalized;
    });
  },

  normalizeBlockItems(type, items) {
    if (!Array.isArray(items)) {
      return [];
    }
    if (type === 'time_slots') {
      return items.map(slot => ({
        ...slot,
        periodLabel: slot.period === 'AFTERNOON' ? '下午' : '上午',
        remainLabel: typeof slot.remain === 'number' ? slot.remain : 0,
        statusLabel: this.formatSlotStatus(slot.status),
        statusClass: this.formatSlotStatusClass(slot.status)
      }));
    }
    if (type === 'booking_records') {
      return items.map(record => ({
        ...record,
        statusLabel: this.formatBookingStatus(record.status),
        checkinLabel: this.formatCheckinStatus(record.checkin)
      }));
    }
    if (type === 'rules_source') {
      return items.map(item => ({
        ...item,
        sourceLabel: this.formatSourceLabel(item.sourceType || '')
      }));
    }
    if (type === 'tips') {
      return items.map(item => {
        if (typeof item === 'string') {
          return { text: item };
        }
        return item;
      });
    }
    return items;
  },

  inferMeta(reply, intent, blocks) {
    const sources = blocks.map(block => block.source);
    if (sources.includes('config')) {
      return {
        state: 'config',
        stateText: '当前后端未配置 AI Key，请先完成配置。'
      };
    }
    if (sources.includes('rag_miss') || sources.includes('rag_disabled')) {
      return {
        state: 'rag-miss',
        stateText: '这类问题当前没有可靠命中，助手已按规则明确说明不知道。'
      };
    }
    if (!this.data.isLoggedIn && intent !== 'RULES' && /登录|未登录|Token/.test(reply)) {
      return {
        state: 'login-required',
        stateText: '涉及预约写操作前需要先登录。',
        showLoginAction: true
      };
    }
    return {
      state: '',
      stateText: ''
    };
  },

  createTipBlock(title, text, source) {
    return {
      type: 'tips',
      title,
      source,
      sourceLabel: this.formatSourceLabel(source),
      blockId: nextId('tip'),
      expanded: true,
      items: [{ text }]
    };
  },

  formatSourceLabel(source) {
    if (source === 'static_rules') {
      return '参观须知';
    }
    if (source === 'news') {
      return '公告';
    }
    if (source === 'tool:queryTimes') {
      return '余票查询';
    }
    if (source === 'tool:listRecords') {
      return '预约记录';
    }
    if (source === 'tool:submitBooking') {
      return '预约结果';
    }
    if (source === 'tool:cancelBooking') {
      return '取消结果';
    }
    if (source === 'config') {
      return '配置提示';
    }
    if (source === 'rag_miss' || source === 'rag_disabled') {
      return '检索提示';
    }
    if (source === 'network') {
      return '网络提示';
    }
    return source || '助手消息';
  },

  formatSlotStatus(status) {
    if (status === 'FULL') {
      return '已约满';
    }
    if (status === 'AVAILABLE') {
      return '可预约';
    }
    return status || '未知';
  },

  formatSlotStatusClass(status) {
    if (status === 'FULL') {
      return 'status-full';
    }
    if (status === 'AVAILABLE') {
      return 'status-available';
    }
    return 'status-unknown';
  },

  formatBookingStatus(status) {
    if (status === 'BOOKED') {
      return '已预约';
    }
    if (status === 'CANCELLED') {
      return '已取消';
    }
    return status || '未知';
  },

  formatCheckinStatus(status) {
    if (status === 'CHECKED_IN') {
      return '已核销';
    }
    if (status === 'EXPIRED') {
      return '已失效';
    }
    if (status === 'UNCHECKED') {
      return '待入馆';
    }
    return status || '未知';
  },

  scrollToBottom() {
    const messages = this.data.messages;
    if (!messages.length) {
      return;
    }
    this.setData({
      toView: messages[messages.length - 1].id
    });
  }
});
