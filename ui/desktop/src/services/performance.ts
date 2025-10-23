// Performance monitoring and optimization utilities

export interface PerformanceMetric {
  name: string;
  startTime: number;
  endTime?: number;
  duration?: number;
  metadata?: Record<string, any>;
}

export interface PerformanceReport {
  metrics: PerformanceMetric[];
  averageResponseTime: number;
  slowestOperation: PerformanceMetric | null;
  totalOperations: number;
}

export class PerformanceMonitor {
  private static instance: PerformanceMonitor;
  private metrics: PerformanceMetric[] = [];
  private maxMetrics = 100;
  private slowThreshold = 200; // 200ms threshold

  static getInstance(): PerformanceMonitor {
    if (!PerformanceMonitor.instance) {
      PerformanceMonitor.instance = new PerformanceMonitor();
    }
    return PerformanceMonitor.instance;
  }

  // Start timing an operation
  startTiming(name: string, metadata?: Record<string, any>): string {
    const id = `${name}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const metric: PerformanceMetric = {
      name,
      startTime: performance.now(),
      metadata
    };
    
    this.metrics.push(metric);
    
    // Maintain metrics size
    if (this.metrics.length > this.maxMetrics) {
      this.metrics.shift();
    }
    
    return id;
  }

  // End timing an operation
  endTiming(id: string): PerformanceMetric | null {
    const metric = this.metrics.find(m => m.name === id.split('_')[0] && !m.endTime);
    if (!metric) return null;

    metric.endTime = performance.now();
    metric.duration = metric.endTime - metric.startTime;
    
    // Log slow operations
    if (metric.duration > this.slowThreshold) {
      console.warn(`Slow operation detected: ${metric.name} took ${metric.duration.toFixed(2)}ms`);
    }
    
    return metric;
  }

  // Measure async operation
  async measureAsync<T>(
    name: string,
    operation: () => Promise<T>,
    metadata?: Record<string, any>
  ): Promise<T> {
    const id = this.startTiming(name, metadata);
    try {
      const result = await operation();
      this.endTiming(id);
      return result;
    } catch (error) {
      this.endTiming(id);
      throw error;
    }
  }

  // Get performance report
  getReport(): PerformanceReport {
    const completedMetrics = this.metrics.filter(m => m.duration !== undefined);
    
    if (completedMetrics.length === 0) {
      return {
        metrics: [],
        averageResponseTime: 0,
        slowestOperation: null,
        totalOperations: 0
      };
    }

    const totalDuration = completedMetrics.reduce((sum, m) => sum + (m.duration || 0), 0);
    const averageResponseTime = totalDuration / completedMetrics.length;
    const slowestOperation = completedMetrics.reduce((slowest, current) => 
      (current.duration || 0) > (slowest.duration || 0) ? current : slowest
    );

    return {
      metrics: completedMetrics,
      averageResponseTime,
      slowestOperation,
      totalOperations: completedMetrics.length
    };
  }

  // Get slow operations
  getSlowOperations(threshold: number = this.slowThreshold): PerformanceMetric[] {
    return this.metrics.filter(m => m.duration && m.duration > threshold);
  }

  // Clear metrics
  clearMetrics(): void {
    this.metrics = [];
  }
}

// Export singleton instance
export const performanceMonitor = PerformanceMonitor.getInstance();

// Performance optimization utilities
export class PerformanceOptimizer {
  // Debounce function calls
  static debounce<T extends (...args: any[]) => any>(
    func: T,
    wait: number
  ): (...args: Parameters<T>) => void {
    let timeout: NodeJS.Timeout;
    return (...args: Parameters<T>) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => func(...args), wait);
    };
  }

  // Throttle function calls
  static throttle<T extends (...args: any[]) => any>(
    func: T,
    limit: number
  ): (...args: Parameters<T>) => void {
    let inThrottle: boolean;
    return (...args: Parameters<T>) => {
      if (!inThrottle) {
        func(...args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  }

  // Memoize function results
  static memoize<T extends (...args: any[]) => any>(
    func: T,
    keyGenerator?: (...args: Parameters<T>) => string
  ): T {
    const cache = new Map<string, ReturnType<T>>();
    
    return ((...args: Parameters<T>) => {
      const key = keyGenerator ? keyGenerator(...args) : JSON.stringify(args);
      
      if (cache.has(key)) {
        return cache.get(key);
      }
      
      const result = func(...args);
      cache.set(key, result);
      return result;
    }) as T;
  }

  // Lazy load components
  static lazyLoad<T extends React.ComponentType<any>>(
    importFunc: () => Promise<{ default: T }>
  ): React.LazyExoticComponent<T> {
    return React.lazy(importFunc);
  }

  // Virtual scrolling for large lists
  static createVirtualList<T>(
    items: T[],
    itemHeight: number,
    containerHeight: number,
    renderItem: (item: T, index: number) => React.ReactNode
  ) {
    const visibleCount = Math.ceil(containerHeight / itemHeight);
    const startIndex = 0; // This would be calculated based on scroll position
    const endIndex = Math.min(startIndex + visibleCount, items.length);
    
    return items.slice(startIndex, endIndex).map((item, index) => 
      renderItem(item, startIndex + index)
    );
  }
}

// React performance hooks
export const usePerformance = () => {
  const [metrics, setMetrics] = React.useState<PerformanceMetric[]>([]);
  
  React.useEffect(() => {
    const interval = setInterval(() => {
      setMetrics([...performanceMonitor.getReport().metrics]);
    }, 1000);
    
    return () => clearInterval(interval);
  }, []);
  
  return {
    metrics,
    report: performanceMonitor.getReport(),
    slowOperations: performanceMonitor.getSlowOperations()
  };
};

// Performance monitoring hook
export const usePerformanceMonitor = (operationName: string) => {
  const [isLoading, setIsLoading] = React.useState(false);
  const [duration, setDuration] = React.useState<number | null>(null);
  
  const measure = React.useCallback(async <T>(
    operation: () => Promise<T>
  ): Promise<T> => {
    setIsLoading(true);
    setDuration(null);
    
    try {
      const result = await performanceMonitor.measureAsync(
        operationName,
        operation
      );
      
      const report = performanceMonitor.getReport();
      const latestMetric = report.metrics[report.metrics.length - 1];
      setDuration(latestMetric?.duration || null);
      
      return result;
    } finally {
      setIsLoading(false);
    }
  }, [operationName]);
  
  return { measure, isLoading, duration };
};

// Import React for hooks
import React from 'react';
