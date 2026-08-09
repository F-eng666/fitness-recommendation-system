<template>
  <div class="wallet-page">
    <h2 class="page-title">我的钱包</h2>

    <!-- 余额卡片 -->
    <div class="balance-card">
      <div class="balance-header">账户余额</div>
      <div class="balance-amount">¥{{ balance.toFixed(2) }}</div>
      <div class="balance-actions">
        <el-button type="primary" size="large" @click="showRechargeDialog = true">
          充值
        </el-button>
      </div>
    </div>

    <!-- 交易记录 -->
    <div class="transactions-section">
      <h3>交易记录</h3>
      <div v-loading="loading">
        <el-empty v-if="!loading && txList.length === 0" description="暂无交易记录" />
        <el-table v-else :data="txList" stripe>
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="txTypeMap[row.type] || 'info'" size="small">
                {{ txLabelMap[row.type] || row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">
              <span :style="{ color: (isIncome(row.type)) ? '#67c23a' : '#f56c6c', fontWeight: 600 }">
                {{ isIncome(row.type) ? '+' : '-' }}¥{{ Math.abs(row.amount || 0).toFixed(2) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="balanceBefore" label="交易前" width="120">
            <template #default="{ row }">¥{{ row.balanceBefore?.toFixed(2) || '0.00' }}</template>
          </el-table-column>
          <el-table-column prop="balanceAfter" label="交易后" width="120">
            <template #default="{ row }">¥{{ row.balanceAfter?.toFixed(2) || '0.00' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
      </div>
    </div>

    <!-- 充值对话框 -->
    <el-dialog v-model="showRechargeDialog" title="余额充值" width="400px" destroy-on-close>
      <div class="recharge-options">
        <div class="amount-preset">
          <el-button
            v-for="amt in presetAmounts"
            :key="amt"
            :type="rechargeAmount === amt ? 'primary' : 'default'"
            @click="rechargeAmount = amt"
          >
            ¥{{ amt }}
          </el-button>
        </div>
        <el-input
          v-model.number="rechargeAmount"
          type="number"
          placeholder="自定义金额"
          size="large"
          style="margin-top: 16px"
        >
          <template #prefix>¥</template>
        </el-input>
      </div>
      <template #footer>
        <el-button @click="showRechargeDialog = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="handleRecharge">
          确认充值
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyWallet, getWalletTransactions, rechargeWallet } from '@/api/wallet'

const balance = ref(0)
const txList = ref([])
const loading = ref(false)

const showRechargeDialog = ref(false)
const rechargeAmount = ref(100)
const recharging = ref(false)
const presetAmounts = [50, 100, 200, 500, 1000]

const txTypeMap = { RECHARGE: 'success', PAY: 'danger', REFUND: 'warning', PURCHASE_CARD: 'warning' }
const txLabelMap = { RECHARGE: '充值', PAY: '消费', REFUND: '退款', PURCHASE_CARD: '购买会员卡' }
const isIncome = (type) => type === 'RECHARGE' || type === 'REFUND'

async function fetchWallet() {
  try {
    const res = await getMyWallet()
    const data = res?.data || res
    balance.value = data.balance || 0
  } catch (err) {
    console.error('获取钱包信息失败:', err)
  }
}

async function fetchTransactions() {
  loading.value = true
  try {
    const res = await getWalletTransactions({ page: 1, size: 50 })
    txList.value = res?.data?.records || res?.data?.list || res?.data || []
  } catch (err) {
    console.error('获取交易记录失败:', err)
  } finally {
    loading.value = false
  }
}

async function handleRecharge() {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    ElMessage.warning('请输入有效的充值金额')
    return
  }
  recharging.value = true
  try {
    await rechargeWallet(rechargeAmount.value)
    ElMessage.success(`充值 ¥${rechargeAmount.value} 成功！`)
    showRechargeDialog.value = false
    rechargeAmount.value = 100
    fetchWallet()
    fetchTransactions()
  } catch (err) {
    console.error('充值失败:', err)
  } finally {
    recharging.value = false
  }
}

onMounted(() => {
  fetchWallet()
  fetchTransactions()
})
</script>

<style scoped>
.wallet-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px 40px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 24px 0;
}

.balance-card {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 12px;
  padding: 32px;
  color: #fff;
  text-align: center;
  margin-bottom: 32px;
}

.balance-header {
  font-size: 14px;
  opacity: 0.8;
  margin-bottom: 8px;
}

.balance-amount {
  font-size: 42px;
  font-weight: 800;
  margin-bottom: 16px;
  font-family: 'DIN', 'Helvetica Neue', sans-serif;
}

.transactions-section {
  margin-top: 8px;
}

.transactions-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.recharge-options {
  text-align: center;
}

.amount-preset {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.amount-preset .el-button {
  min-width: 72px;
}
</style>
