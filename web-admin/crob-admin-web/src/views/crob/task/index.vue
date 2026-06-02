<template>
  <div class="p-16px">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-16px font-600">任务列表</span>
          <el-button type="primary" @click="$router.push('/crob/chat')">新建任务</el-button>
        </div>
      </template>

      <el-table :data="taskList" stripe v-loading="loading" @row-click="openDetail">
        <el-table-column prop="taskId" label="ID" width="160" />
        <el-table-column prop="scenario" label="场景" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ row.scenario }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" min-width="200">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click.stop="openChat(row)">
              查看
            </el-button>
            <el-button v-if="row.status === 'SUCCEEDED'" size="small" text type="success"
              @click.stop="openReport(row)">
              报告
            </el-button>
            <el-button v-if="row.status === 'RUNNING'" size="small" text type="danger"
              @click.stop="cancelTask(row)">
              取消
            </el-button>
            <el-button v-if="row.status === 'SUCCEEDED' || row.status === 'FAILED'"
              size="small" text @click.stop="rerunTask(row)">
              重跑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

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
  return cleaned.startsWith('/') ? cleaned : `/${cleaned}`
})
const apiRoot = computed(() => `${apiBaseUrl.value}${apiPrefix.value}`)

const router = useRouter()
const taskList = ref<any[]>([])
const loading = ref(false)

const statusColor = (s: string) => {
  const m: Record<string, string> = { SUCCEEDED: 'success', FAILED: 'danger', RUNNING: 'warning', QUEUED: '', WAITING_INPUT: 'info', CANCELLED: 'info', REJECTED: 'danger' }
  return m[s] || ''
}

const fetchTasks = async () => {
  loading.value = true
  try {
    // fetch recent tasks from known IDs (MVP: simple approach)
    // In production, this should be a proper list API
    const stored = sessionStorage.getItem('crob_task_ids')
    const ids: number[] = stored ? JSON.parse(stored) : []
    const results: any[] = []
    for (const id of ids) {
      try {
        const resp = await fetch(`${apiRoot.value}/tasks/${id}`)
        if (resp.ok) {
          const json = await resp.json()
          if (json?.data) results.push(json.data)
        }
      } catch { /* skip */ }
    }
    taskList.value = results
  } finally {
    loading.value = false
  }
}

const openChat = (row: any) => router.push('/crob/chat')
const openDetail = (row: any) => openChat(row)

const openReport = (row: any) => {
  const url = `${apiBaseUrl.value}/tasks/${row.taskId}/report?attempt_id=${row.currentAttemptId || row.latestSuccessAttemptId}`
  window.open(url, '_blank')
}

const cancelTask = async (row: any) => {
  const aid = row.currentAttemptId
  if (!aid) return
  await fetch(`${apiRoot.value}/tasks/${row.taskId}/cancel?attempt_id=${aid}`, { method: 'POST' })
  ElMessage.success('已取消')
  fetchTasks()
}

const rerunTask = async (row: any) => {
  await fetch(`${apiRoot.value}/tasks/${row.taskId}/rerun`, { method: 'POST' })
  ElMessage.success('已提交重跑')
  fetchTasks()
}

onMounted(fetchTasks)
</script>
