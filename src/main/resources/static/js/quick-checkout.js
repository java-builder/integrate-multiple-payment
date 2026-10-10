let quickProduct = null;
let quickGateway = 'VNPAY';
let quickCurrency = 'USD';
const USD_VND_RATE = 25000;

const QUICK_GATEWAY_CONFIGS = {
    VNPAY: {
        activeClass: 'vnpay-active',
        btnClass: 'quick-pay-btn btn-vnpay',
        btnText: 'Thanh toán qua VNPAY',
        lockCurrency: null,
        hintText: null
    },
    PAYPAL: {
        activeClass: 'paypal-active',
        btnClass: 'quick-pay-btn btn-paypal',
        btnText: 'Thanh toán qua PayPal',
        lockCurrency: 'USD',
        hintText: 'PayPal chỉ hỗ trợ thanh toán bằng Đô la Mỹ (USD)'
    },
    STRIPE: {
        activeClass: null,
        btnClass: 'quick-pay-btn btn-stripe',
        btnText: 'Thanh toán qua Stripe',
        lockCurrency: null,
        hintText: null
    }
};

function formatQuickCurrency(amount, currency) {
    if (currency === 'VND') {
        const vnd = amount * USD_VND_RATE;
        return new Intl.NumberFormat('vi-VN').format(Math.round(vnd)) + ' ₫';
    }
    return '$' + Number(amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function openQuickCheckout(btn) {
    const id = Number.parseInt(btn.dataset.id, 10);
    const name = btn.dataset.name;
    const price = Number.parseFloat(btn.dataset.price);
    const image = btn.dataset.image || '';

    quickProduct = {
        id,
        name,
        price,
        image,
        quantity: 1
    };

    // Render in modal
    document.getElementById('quick-prod-title').textContent = name;
    document.getElementById('quick-stepper-value').textContent = '1';

    const imgEl = document.getElementById('quick-prod-image');
    if (imgEl) {
        imgEl.src = image;
        imgEl.style.display = image ? 'block' : 'none';
    }

    // Default to USD and VNPAY
    selectQuickGateway('VNPAY');
    setQuickCurrency('USD');
    updateQuickTotals();

    // Show modal
    const backdrop = document.getElementById('quick-checkout-modal');
    if (backdrop) {
        backdrop.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closeQuickModal() {
    const backdrop = document.getElementById('quick-checkout-modal');
    if (backdrop) {
        backdrop.classList.remove('active');
        document.body.style.overflow = '';
    }
    closeQuickDropdown();
}

function changeQuickQty(delta) {
    if (!quickProduct) return;
    const newQty = quickProduct.quantity + delta;
    if (newQty < 1) return;

    quickProduct.quantity = newQty;
    document.getElementById('quick-stepper-value').textContent = newQty;
    updateQuickTotals();
}

/* Custom Currency Dropdown */
function toggleQuickCurrencyDropdown() {
    const trigger = document.getElementById('quick-curr-trigger');
    if (trigger?.classList.contains('disabled')) return;
    const wrapper = document.getElementById('quick-curr-wrapper');
    wrapper?.classList.toggle('open');
}

function closeQuickDropdown() {
    const wrapper = document.getElementById('quick-curr-wrapper');
    wrapper?.classList.remove('open');
}

function setQuickCurrency(curr) {
    closeQuickDropdown();

    if (quickGateway === 'PAYPAL' && curr === 'VND') {
        showToast('PayPal chỉ hỗ trợ thanh toán bằng Đô la Mỹ (USD)');
        return;
    }

    quickCurrency = curr;

    const displayEl = document.getElementById('quick-curr-display');
    const optVnd = document.getElementById('quick-opt-vnd');
    const optUsd = document.getElementById('quick-opt-usd');

    if (displayEl) {
        if (curr === 'VND') {
            displayEl.innerHTML = `
                <span class="quick-curr-badge quick-badge-vnd">₫</span>
                <div>
                    <span class="quick-opt-title">VND (₫)</span>
                    <span class="quick-opt-sub" style="margin-left: 4px;">- Việt Nam Đồng</span>
                </div>
            `;
            optVnd?.classList.add('selected');
            optUsd?.classList.remove('selected');
        } else {
            displayEl.innerHTML = `
                <span class="quick-curr-badge quick-badge-usd">$</span>
                <div>
                    <span class="quick-opt-title">USD ($)</span>
                    <span class="quick-opt-sub" style="margin-left: 4px;">- Đô la Mỹ</span>
                </div>
            `;
            optUsd?.classList.add('selected');
            optVnd?.classList.remove('selected');
        }
    }

    updateQuickTotals();
}

function selectQuickGateway(gateway) {
    if (gateway === 'VIETQR') {
        showQuickNotice('VietQR Napas 247');
        return;
    }

    const config = QUICK_GATEWAY_CONFIGS[gateway];
    if (!config) return;

    quickGateway = gateway;

    const vnpayItem = document.getElementById('quick-gw-vnpay');
    const stripeItem = document.getElementById('quick-gw-stripe');
    const paypalItem = document.getElementById('quick-gw-paypal');
    const currTrigger = document.getElementById('quick-curr-trigger');
    const currHint = document.getElementById('quick-currency-hint');
    const payBtn = document.getElementById('quick-pay-submit-btn');
    const payText = document.getElementById('quick-pay-btn-text');

    // Reset active states
    [vnpayItem, stripeItem, paypalItem].forEach(el => {
        el?.classList.remove('active', 'vnpay-active', 'paypal-active');
    });

    const activeItem = document.getElementById(`quick-gw-${gateway.toLowerCase()}`);
    if (activeItem) {
        activeItem.classList.add('active');
        if (config.activeClass) activeItem.classList.add(config.activeClass);
    }

    if (config.lockCurrency) {
        setQuickCurrency(config.lockCurrency);
        currTrigger?.classList.add('disabled');
    } else {
        currTrigger?.classList.remove('disabled');
    }

    if (currHint) {
        if (config.hintText) {
            currHint.innerHTML = `<i class="fa-solid fa-circle-info"></i> ${config.hintText}`;
            currHint.style.display = 'flex';
        } else {
            currHint.style.display = 'none';
        }
    }

    if (payBtn) payBtn.className = config.btnClass;
    if (payText) payText.textContent = config.btnText;

    updateQuickTotals();
}

function updateQuickTotals() {
    if (!quickProduct) return;
    const subtotal = quickProduct.price * quickProduct.quantity;

    const unitPriceEl = document.getElementById('quick-prod-unitprice');
    const totalAmountEl = document.getElementById('quick-total-display');

    if (unitPriceEl) unitPriceEl.textContent = formatQuickCurrency(quickProduct.price, quickCurrency);
    if (totalAmountEl) totalAmountEl.textContent = formatQuickCurrency(subtotal, quickCurrency);
}

function showQuickNotice(gatewayName) {
    const msg = `Cổng ${gatewayName} đang trong quá trình tích hợp. Vui lòng chọn VNPAY hoặc Stripe!`;
    if (typeof showToast === 'function') {
        showToast(msg);
    } else {
        alert(msg);
    }
}

document.addEventListener('click', (e) => {
    const wrapper = document.getElementById('quick-curr-wrapper');
    if (!wrapper?.contains(e.target)) {
        closeQuickDropdown();
    }
});

async function executeDirectPayment() {
    if (!quickProduct) return;

    const overlay = document.getElementById('quick-loading-overlay');
    const loadingText = document.getElementById('quick-loading-text');

    if (loadingText) {
        loadingText.textContent = `Đang kết nối cổng thanh toán ${quickGateway}...`;
    }
    overlay?.classList.add('active');

    const orderPayload = {
        items: [{
            productId: quickProduct.id,
            quantity: quickProduct.quantity
        }],
        currency: quickCurrency
    };

    try {
        const orderRes = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderPayload)
        });

        if (!orderRes.ok) {
            overlay?.classList.remove('active');
            if (typeof showToast === 'function') showToast('Không thể tạo đơn hàng. Vui lòng thử lại!');
            return;
        }

        const orderData = await orderRes.json();
        const orderCode = orderData?.data?.orderCode;

        const paymentRes = await fetch('/api/payments/initiate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                orderCode,
                paymentGateway: quickGateway
            })
        });

        if (!paymentRes.ok) {
            let errMsg = `Không thể kết nối cổng ${quickGateway}!`;
            try {
                const errData = await paymentRes.json();
                if (errData?.message) errMsg = errData.message;
            } catch {
                // Ignore parse errors for non-JSON responses
            }
            overlay?.classList.remove('active');
            if (typeof showToast === 'function') showToast(errMsg);
            return;
        }

        const paymentData = await paymentRes.json();
        if (paymentData?.data?.paymentUrl) {
            window.location.href = paymentData.data.paymentUrl;
        }

    } catch (err) {
        overlay?.classList.remove('active');
        if (typeof showToast === 'function') showToast('Lỗi kết nối máy chủ. Vui lòng thử lại!');
    }
}
