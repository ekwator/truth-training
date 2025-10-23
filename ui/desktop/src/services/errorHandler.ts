// Error handling utilities

export interface ErrorContext {
  operation: string;
  component?: string;
  userId?: string;
  timestamp: string;
  userAgent: string;
  url?: string;
}

export interface ErrorReport {
  error: Error;
  context: ErrorContext;
  severity: 'low' | 'medium' | 'high' | 'critical';
  userImpact: 'none' | 'minor' | 'moderate' | 'severe';
  retryable: boolean;
}

export class ErrorHandler {
  private static instance: ErrorHandler;
  private errorQueue: ErrorReport[] = [];
  private maxQueueSize = 50;

  static getInstance(): ErrorHandler {
    if (!ErrorHandler.instance) {
      ErrorHandler.instance = new ErrorHandler();
    }
    return ErrorHandler.instance;
  }

  // Handle different types of errors
  handleError(error: Error, context: Partial<ErrorContext> = {}): ErrorReport {
    const errorContext: ErrorContext = {
      operation: 'unknown',
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      ...context
    };

    const report = this.analyzeError(error, errorContext);
    this.queueError(report);
    
    // Log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.error('Error handled:', report);
    }

    return report;
  }

  // Analyze error to determine severity and impact
  private analyzeError(error: Error, context: ErrorContext): ErrorReport {
    let severity: ErrorReport['severity'] = 'low';
    let userImpact: ErrorReport['userImpact'] = 'none';
    let retryable = false;

    // Network errors
    if (error.name === 'NetworkError' || error.message.includes('fetch')) {
      severity = 'medium';
      userImpact = 'moderate';
      retryable = true;
    }
    // API errors
    else if (error.message.includes('API') || error.message.includes('HTTP')) {
      const statusMatch = error.message.match(/(\d{3})/);
      if (statusMatch) {
        const status = parseInt(statusMatch[1]);
        if (status >= 500) {
          severity = 'high';
          userImpact = 'moderate';
          retryable = true;
        } else if (status >= 400) {
          severity = 'medium';
          userImpact = 'minor';
          retryable = false;
        }
      }
    }
    // Validation errors
    else if (error.name === 'ValidationError' || error.message.includes('validation')) {
      severity = 'low';
      userImpact = 'minor';
      retryable = false;
    }
    // Critical errors
    else if (error.name === 'TypeError' || error.name === 'ReferenceError') {
      severity = 'critical';
      userImpact = 'severe';
      retryable = false;
    }

    return {
      error,
      context,
      severity,
      userImpact,
      retryable
    };
  }

  // Queue error for reporting
  private queueError(report: ErrorReport): void {
    this.errorQueue.push(report);
    
    // Maintain queue size
    if (this.errorQueue.length > this.maxQueueSize) {
      this.errorQueue.shift();
    }

    // Auto-report critical errors
    if (report.severity === 'critical') {
      this.reportErrors([report]);
    }
  }

  // Get user-friendly error message
  getUserFriendlyMessage(error: Error, _context: ErrorContext): string {
    // Network errors
    if (error.name === 'NetworkError' || error.message.includes('fetch')) {
      return 'Unable to connect to the server. Please check your internet connection and try again.';
    }
    
    // API errors
    if (error.message.includes('API') || error.message.includes('HTTP')) {
      const statusMatch = error.message.match(/(\d{3})/);
      if (statusMatch) {
        const status = parseInt(statusMatch[1]);
        if (status >= 500) {
          return 'The server is experiencing issues. Please try again later.';
        } else if (status === 404) {
          return 'The requested resource was not found.';
        } else if (status === 403) {
          return 'You do not have permission to perform this action.';
        } else if (status === 401) {
          return 'Please log in to continue.';
        }
      }
      return 'An error occurred while communicating with the server.';
    }
    
    // Validation errors
    if (error.name === 'ValidationError' || error.message.includes('validation')) {
      return 'Please check your input and try again.';
    }
    
    // Default message
    return 'An unexpected error occurred. Please try again.';
  }

  // Get retry suggestion
  getRetrySuggestion(error: Error): { shouldRetry: boolean; delay?: number } {
    if (error.name === 'NetworkError' || error.message.includes('fetch')) {
      return { shouldRetry: true, delay: 2000 };
    }
    
    if (error.message.includes('HTTP 5')) {
      return { shouldRetry: true, delay: 5000 };
    }
    
    if (error.message.includes('HTTP 429')) {
      return { shouldRetry: true, delay: 10000 };
    }
    
    return { shouldRetry: false };
  }

  // Report errors to external service
  async reportErrors(reports: ErrorReport[] = this.errorQueue): Promise<void> {
    if (reports.length === 0) return;

    try {
      // In a real application, this would send to an error reporting service
      console.log('Reporting errors:', reports);
      
      // Clear reported errors
      this.errorQueue = this.errorQueue.filter(
        queued => !reports.some(reported => reported === queued)
      );
    } catch (reportingError) {
      console.error('Failed to report errors:', reportingError);
    }
  }

  // Get error statistics
  getErrorStats(): {
    totalErrors: number;
    errorsBySeverity: Record<string, number>;
    errorsByImpact: Record<string, number>;
    retryableErrors: number;
  } {
    const stats = {
      totalErrors: this.errorQueue.length,
      errorsBySeverity: {} as Record<string, number>,
      errorsByImpact: {} as Record<string, number>,
      retryableErrors: 0
    };

    this.errorQueue.forEach(report => {
      stats.errorsBySeverity[report.severity] = (stats.errorsBySeverity[report.severity] || 0) + 1;
      stats.errorsByImpact[report.userImpact] = (stats.errorsByImpact[report.userImpact] || 0) + 1;
      if (report.retryable) stats.retryableErrors++;
    });

    return stats;
  }

  // Clear error queue
  clearErrors(): void {
    this.errorQueue = [];
  }

  // Get queued errors
  getQueuedErrors(): ErrorReport[] {
    return [...this.errorQueue];
  }
}

// Export singleton instance
export const errorHandler = ErrorHandler.getInstance();

// Global error handler for unhandled errors
if (typeof window !== 'undefined') {
  window.addEventListener('error', (event) => {
    errorHandler.handleError(event.error, {
      operation: 'unhandled_error',
      component: 'global'
    });
  });

  window.addEventListener('unhandledrejection', (event) => {
    errorHandler.handleError(
      new Error(event.reason?.message || 'Unhandled promise rejection'),
      {
        operation: 'unhandled_promise_rejection',
        component: 'global'
      }
    );
  });
}
