<template>
  <div class="app">
    <h1>Text2SQL 演示</h1>
    
    <div class="section">
      <h2>文本转 SQL</h2>
      <div class="form">
        <textarea 
          v-model="queryText" 
          placeholder="输入自然语言查询，例如：查询所有用户信息\n查询年龄大于30的用户\n查询订单金额大于1000的订单\n查询每个用户的订单数量和总金额" 
        ></textarea>
        <div class="example-queries">
          <h3>示例查询：</h3>
          <div class="example-buttons">
            <button 
              v-for="example in examples" 
              :key="example" 
              class="example-btn"
              @click="queryText = example"
            >
              {{ example }}
            </button>
          </div>
        </div>
        <button @click="convertTextToSql" :disabled="loading">
          {{ loading ? '转换中...' : '转换为 SQL' }}
        </button>
        <div v-if="result" class="result">
          <h3>转换结果：</h3>
          <p><strong>原始查询：</strong>{{ result.originalText }}</p>
          <p><strong>生成的 SQL：</strong></p>
          <div class="sql-code">{{ result.generatedSql }}</div>
          <p><strong>查询结果：</strong></p>
          <table v-if="result.results && result.results.length > 0" class="results-table">
            <thead>
              <tr>
                <th v-for="(key, index) in Object.keys(result.results[0])" :key="index">
                  {{ key }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rowIndex) in result.results" :key="rowIndex">
                <td v-for="(value, key) in row" :key="key">
                  {{ value }}
                </td>
              </tr>
            </tbody>
          </table>
          <p v-else>无查询结果</p>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <div class="section">
      <h2>数据库表结构</h2>
      <div class="database-schema">
        <div class="schema-table">
          <h4>users 表</h4>
          <p>id (integer): 用户ID</p>
          <p>name (varchar): 用户名</p>
          <p>email (varchar): 邮箱</p>
          <p>age (integer): 年龄</p>
          <p>created_at (timestamp): 创建时间</p>
        </div>
        <div class="schema-table">
          <h4>orders 表</h4>
          <p>id (integer): 订单ID</p>
          <p>user_id (integer): 用户ID</p>
          <p>amount (decimal): 订单金额</p>
          <p>status (varchar): 订单状态</p>
          <p>created_at (timestamp): 创建时间</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'App',
  data() {
    return {
      queryText: '',
      result: null,
      error: null,
      loading: false,
      examples: [
        '查询所有用户信息',
        '查询年龄大于30的用户',
        '查询订单金额大于1000的订单',
        '查询每个用户的订单数量和总金额',
        '查询最近一周创建的订单',
        '查询邮箱包含gmail的用户'
      ]
    };
  },
  methods: {
    async convertTextToSql() {
      if (!this.queryText.trim()) {
        this.error = '查询文本不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.result = null;
      
      try {
        const response = await axios.post('/api/text2sql/convert', {
          text: this.queryText
        });
        this.result = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '转换失败';
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
