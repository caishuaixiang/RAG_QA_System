import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`
      }
    }
  },
  // 构建优化配置
  build: {
    // 启用代码压缩
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 移除console.log
        drop_debugger: true, // 移除debugger
        pure_funcs: ['console.log'] // 移除特定的console调用
      }
    },
    
    // 分块策略
    rollupOptions: {
      output: {
        manualChunks: {
          // 将Element Plus单独分块
          'element-plus': ['element-plus'],
          // 将Vue相关库单独分块
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // 将工具库单独分块
          'utils': ['axios', 'dayjs', 'js-cookie'],
          // 将图表库单独分块
          'charts': ['echarts'],
          // 将图标库单独分块
          'icons': ['@element-plus/icons-vue']
        },
        // 资源分块
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name?.split('.')
          const ext = info?.[1]
          if (ext === 'css') {
            return 'assets/css/[name]-[hash][extname]'
          }
          if (ext === 'jpg' || ext === 'jpeg' || ext === 'png' || ext === 'gif' || ext === 'svg') {
            return 'assets/images/[name]-[hash][extname]'
          }
          return 'assets/[name]-[hash][extname]'
        }
      }
    },
    
    // 构建时显示警告
    chunkSizeWarningLimit: 1000,
    
    // 启用源码映射（生产环境建议关闭）
    sourcemap: false,
    
    // 启用CSS代码分割
    cssCodeSplit: true,
    
    // 构建时生成包大小分析
    reportCompressedSize: false
  },
  
  // 开发服务器优化
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'element-plus',
      '@element-plus/icons-vue',
      'axios',
      'dayjs',
      'js-cookie',
      'echarts'
    ],
    exclude: ['@iconify/json']
  },
  
  // 预构建优化
  cacheDir: 'node_modules/.vite'
})