<template>
  <div class="p-16px h-full flex flex-col gap-12px">
    <el-card shadow="never" class="flex-1 min-h-0 flex flex-col"
      :body-style="{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-12px">
            <span class="text-16px font-600">Agent 对话</span>
            <el-select v-model="scenario" :disabled="isRunning" style="width:160px">
              <el-option label="自由对话" value="CHAT" />
              <el-option label="市场分析" value="MARKET_ANALYSIS" />
              <el-option label="利润测算" value="PROFIT_MODEL" />
            </el-select>
            <el-tag v-if="taskId" size="small" :type="taskStatusColor">
              {{ taskStatus || 'idle' }}
            </el-tag>
          </div>
          <div class="flex items-center gap-8px">
            <el-button :disabled="!canSend" type="primary" @click="send">发送</el-button>
            <el-button :disabled="!isRunning" @click="stop">停止</el-button>
          </div>
        </div>
      </template>

      <el-scrollbar ref="scrollbarRef" class="flex-1 min-h-0 pr-8px">
        <!-- structured fields -->
        <div v-if="scenario !== 'CHAT'" class="mb-12px p-10px bg-[var(--el-fill-color-lighter)] rounded-8px">
          <div class="text-13px font-600 mb-8px">场景参数</div>
          <template v-if="scenario === 'MARKET_ANALYSIS'">
            <el-row :gutter="8">
              <el-col :span="8">
                <el-select v-model="fields.platform" :disabled="isRunning" placeholder="平台" clearable>
                  <el-option label="Amazon" value="AMAZON" />
                  <el-option label="Temu" value="TEMU" />
                  <el-option label="TikTok Shop" value="TIKTOK_SHOP" />
                </el-select>
              </el-col>
              <el-col :span="8">
                <el-select v-model="fields.market" :disabled="isRunning" placeholder="市场" clearable>
                  <el-option label="US" value="US" />
                  <el-option label="UK" value="UK" />
                  <el-option label="DE" value="DE" />
                  <el-option label="FR" value="FR" />
                  <el-option label="JP" value="JP" />
                </el-select>
              </el-col>
              <el-col :span="8">
                <el-input v-model="fields.category_or_keyword" :disabled="isRunning" placeholder="品类/关键词" />
              </el-col>
            </el-row>
          </template>
          <template v-if="scenario === 'PROFIT_MODEL'">
            <el-row :gutter="8">
              <el-col :span="6">
                <el-select v-model="fields.platform" :disabled="isRunning" placeholder="平台" clearable>
                  <el-option label="Amazon" value="AMAZON" />
                  <el-option label="Temu" value="TEMU" />
                </el-select>
              </el-col>
              <el-col :span="6">
                <el-select v-model="fields.market" :disabled="isRunning" placeholder="市场" clearable>
                  <el-option label="US" value="US" />
                  <el-option label="UK" value="UK" />
                  <el-option label="DE" value="DE" />
                </el-select>
              </el-col>
              <el-col :span="6">
                <el-select v-model="fields.shipping_mode" :disabled="isRunning" placeholder="物流方式" clearable>
                  <el-option label="FBA" value="FBA" />
                  <el-option label="FBM" value="FBM" />
                </el-select>
              </el-col>
              <el-col :span="3">
                <el-input-number v-model="fields.cost_fen" :disabled="isRunning" :min="0" placeholder="成本(分)" controls-position="right" style="width:100%" />
              </el-col>
              <el-col :span="3">
                <el-input-number v-model="fields.price_fen" :disabled="isRunning" :min="0" placeholder="售价(分)" controls-position="right" style="width:100%" />
              </el-col>
            </el-row>
            <div v-if="confirmNegativeVisible" class="mt-8px text-13px text-orange-600">
              售价低于成本，确认要继续吗？
              <el-button size="small" type="warning" @click="sendWithConfirm">确认</el-button>
              <el-button size="small" @click="confirmNegativeVisible = false">取消</el-button>
            </div>
          </template>
        </div>

        <!-- chat messages -->
        <div class="flex flex-col gap-10px">
          <div v-if="missingFields.length" class="rounded-8px p-10px bg-orange-50 text-orange-700 text-13px">
            缺少参数: {{ missingFields.join(', ') }}，请补充后重新发送。
          </div>
          <div v-for="(m, idx) in messages" :key="idx" class="rounded-8px p-10px"
            :class="m.role === 'user' ? 'bg-[var(--el-color-primary-light-9)]' : 'bg-[var(--el-fill-color-light)]'">
            <div class="text-12px opacity-70 mb-6px">{{ m.role === 'user' ? '你' : 'Agent' }}</div>
            <div class="whitespace-pre-wrap break-words leading-22px">{{ m.content }}</div>
          </div>
        </div>
      </el-scrollbar>

      <div class="mt-12px">
        <el-input v-model="prompt" :autosize="{ minRows: 2, maxRows: 6 }" :disabled="isRunning" maxlength="4000"
          placeholder="输入你的问题，然后点击发送" show-word-limit type="textarea"
          @keydown.enter.exact.prevent="send" />
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { computed, nextTick, onBeforeUnmount, reactive, ref } from 'vue'

type ChatMessage = { role: 'user' | 'assistant'; content: string }

const scenario = ref('CHAT')
const prompt = ref('')
const messages = ref<ChatMessage[]>([])
const isRunning = ref(false)
const abortController = ref<AbortController | null>(null)
const taskId = ref<number | null>(null)
const attemptId = ref<number | null>(null)
const taskStatus = ref('')
const missingFields = ref<string[]>([])
const scrollbarRef = ref<any>(null)
const isAutoScroll = ref(true)
const scrollRafId = ref<number | null>(null)
const confirmNegativeVisible = ref(false)

const fields = reactive<Record<string, any>>({
  platform: '', market: '', category_or_keyword: '',
  shipping_mode: '', cost_fen: undefined, price_fen: undefined
})

const apiBaseUrl = computed(() => {
  const v = (import.meta as any).env?.VITE_BASE_URL
  if (typeof v !== 'string') return ''
  return v.replace(/^['"]|['"]$/g, '').replace(/\/$/, '')
})
const apiPrefix = computed(() => {
  const env = (import.meta as any).env || {}
  const v = env.VITE_CROB_API_URL ?? env.VITE_API_URL
  if (typeof v !== 'string' || !v) return '/api'
  const cleaned = v.replace(/^['"]|['"]$/g, '')
  if (cleaned === '/admin-api') return '/api'
  return cleaned.startsWith('/') ? cleaned : `/${cleaned}`
})
const apiRoot = computed(() => `${apiBaseUrl.value}${apiPrefix.value}`)

const canSend = computed(() => prompt.value.trim() && !isRunning.value)

const taskStatusColor = computed(() => {
  const m: Record<string, string> = { SUCCEEDED: 'success', FAILED: 'danger', RUNNING: 'warning', CANCELLED: 'info' }
  return m[taskStatus.value] || ''
})

const scrollToBottom = async (force = false) => {
  if (!force && !isAutoScroll.value) return
  if (scrollRafId.value != null) return
  scrollRafId.value = requestAnimationFrame(async () => {
    scrollRafId.value = null
    await nextTick()
    const wrap = scrollbarRef.value?.wrapRef
    if (!wrap) return
    wrap.scrollTop = wrap.scrollHeight
  })
}

const handleScroll = () => {
  const wrap = scrollbarRef.value?.wrapRef
  if (!wrap) return
  isAutoScroll.value = (wrap.scrollHeight - (wrap.scrollTop + wrap.clientHeight)) <= 40
}

const buildInputs = (promptText?: string) => {
  const inputs: Record<string, any> = {}
  if (scenario.value === 'MARKET_ANALYSIS') {
    inputs.platform = fields.platform; inputs.market = fields.market; inputs.category_or_keyword = fields.category_or_keyword
  } else if (scenario.value === 'PROFIT_MODEL') {
    inputs.platform = fields.platform; inputs.market = fields.market
    inputs.shipping_mode = fields.shipping_mode
    inputs.cost_fen = fields.cost_fen; inputs.price_fen = fields.price_fen
  }
  const p = (promptText || prompt.value || '').trim()
  if (p) inputs.prompt = p
  return inputs
}

const send = async () => {
  if (!canSend.value) return
  // check negative profit
  if (scenario.value === 'PROFIT_MODEL' && fields.cost_fen != null && fields.price_fen != null) {
    if (fields.price_fen < fields.cost_fen) { confirmNegativeVisible.value = true; return }
  }
  doSend()
}

const sendWithConfirm = () => {
  confirmNegativeVisible.value = false
  fields.confirm_negative_profit = 'YES_CONTINUE'
  doSend()
}

const doSend = async () => {
  missingFields.value = []
  const text = prompt.value.trim()
  const assistantIndex = messages.value.length + 1
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })
  scrollToBottom(true)

  // 先构造 inputs（包含 prompt），再清空文本框
  const inputs = buildInputs(text)
  prompt.value = ''
  isRunning.value = true

  try {
    const createResp = await fetch(`${apiRoot.value}/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ scenario: scenario.value, inputs })
    })
    if (!createResp.ok) throw new Error(`E_HTTP_${createResp.status}`)
    const createJson = await createResp.json()
    const data = createJson?.data
    taskId.value = data?.taskId
    if (taskId.value) {
      const stored = sessionStorage.getItem('crob_task_ids')
      const ids: number[] = stored ? JSON.parse(stored) : []
      if (!ids.includes(taskId.value)) { ids.unshift(taskId.value); sessionStorage.setItem('crob_task_ids', JSON.stringify(ids.slice(0, 50))) }
    }
    attemptId.value = data?.currentAttemptId

    if (data?.disposition === 'ASK_CLARIFY') {
      missingFields.value = data?.missingFields || []
      taskStatus.value = data?.status || 'WAITING_INPUT'
      messages.value[assistantIndex].content = `[需要更多信息] 请补充: ${missingFields.value.join(', ')}`
      isRunning.value = false
      return
    }
    if (data?.disposition === 'REJECT') {
      taskStatus.value = 'REJECTED'
      messages.value[assistantIndex].content = `[请求被拒绝] ${data?.errorMessage || ''}`
      isRunning.value = false
      return
    }
    if (!taskId.value || !attemptId.value) throw new Error('E_CREATE_TASK_FAILED')

    taskStatus.value = 'QUEUED'
    const ctrl = new AbortController()
    abortController.value = ctrl

    await fetchEventSource(`${apiRoot.value}/tasks/${taskId.value}/stream?attempt_id=${attemptId.value}`, {
      signal: ctrl.signal,
      onmessage(ev) {
        if (!ev.data) return
        try {
          const obj = JSON.parse(ev.data)
          const t = obj?.data?.type
          if (t === 'L6_DELTA') {
            messages.value[assistantIndex].content += obj?.data?.delta ?? ''
            scrollToBottom()
          }
        } catch { /* ignore */ }
      },
      onerror(err) { throw err },
      openWhenHidden: true
    })
    // stream ended — poll final status
    await pollStatus()
  } catch (e: any) {
    messages.value[assistantIndex].content += `\n\n[请求失败] ${e?.message || String(e)}`
  } finally {
    isRunning.value = false
    abortController.value = null
    scrollToBottom()
  }
}

const pollStatus = async () => {
  if (!taskId.value) return
  for (let i = 0; i < 30; i++) {
    await new Promise(r => setTimeout(r, 1000))
    try {
      const resp = await fetch(`${apiRoot.value}/tasks/${taskId.value}`)
      const json = await resp.json()
      const st = json?.data?.status
      taskStatus.value = st
      if (st === 'SUCCEEDED' || st === 'FAILED' || st === 'CANCELLED') return
    } catch { /* ignore */ }
  }
}

const stop = async () => {
  if (!abortController.value || !taskId.value || !attemptId.value) return
  try {
    await fetch(`${apiRoot.value}/tasks/${taskId.value}/cancel?attempt_id=${attemptId.value}`, { method: 'POST' })
  } catch { /* ignore */ }
  abortController.value.abort()
  abortController.value = null
  isRunning.value = false
  taskStatus.value = 'CANCELLED'
}

onBeforeUnmount(() => {
  if (scrollRafId.value != null) { cancelAnimationFrame(scrollRafId.value); scrollRafId.value = null }
})
</script>
