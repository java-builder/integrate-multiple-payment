/**
 * Quick Checkout Modal Controller (Thanh toán trực tiếp không cần vào giỏ hàng)
 */

let quickProduct = null;
let quickGateway = 'VNPAY';
let quickCurrency = 'USD';
const USD_VND_RATE = 25000;

function formatQuickCurrency(amount, currency) {
    if (currency === 'VND') {
        const vnd = amount * USD_VND_RATE;
        return new Intl.NumberFormat('vi-VN').format(Math.round(vnd)) + ' ₫';
    }
    return '$' + Number(amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function openQuickCheckout(btn) {
    const id = parseInt(btn.getAttribute('data-id'));
    const name = btn.getAttribute('data-name');
    const price = parseFloat(btn.getAttribute('data-price'));
    const image = btn.getAttribute('data-image') || '';

    quickProduct = {
        id: id,
        name: name,
        price: price,
        image: image,
        quantity: 1
    };

    // Render in modal
    document.getElementById('quick-prod-title').textContent = name;
    document.getElementById('quick-stepper-value').textContent = '1';

    const imgEl = document.getElementById('quick-prod-image');
    if (imgEl) {
        if (image) {
            imgEl.src = image;
            imgEl.style.display = 'block';
        } else {
            imgEl.style.display = 'none';
        }
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
    if (trigger && trigger.classList.contains('disabled')) return;
    const wrapper = document.getElementById('quick-curr-wrapper');
    if (wrapper) wrapper.classList.toggle('open');
}

function closeQuickDropdown() {
    const wrapper = document.getElementById('quick-curr-wrapper');
    if (wrapper) wrapper.classList.remove('open');
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
            if (optVnd) optVnd.classList.add('selected');
            if (optUsd) optUsd.classList.remove('selected');
        } else {
            displayEl.innerHTML = `
                <span class="quick-curr-badge quick-badge-usd">$</span>
                <div>
                    <span class="quick-opt-title">USD ($)</span>
                    <span class="quick-opt-sub" style="margin-left: 4px;">- Đô la Mỹ</span>
                </div>
            `;
            if (optUsd) optUsd.classList.add('selected');
            if (optVnd) optVnd.classList.remove('selected');
        }
    }

    updateQuickTotals();
}

function selectQuickGateway(gateway) {
    if (gateway === 'VIETQR') {
        showQuickNotice('VietQR Napas 247');
        return;
    }

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
        if (el) el.classList.remove('active', 'vnpay-active', 'paypal-active');
    });

    if (gateway === 'VNPAY') {
        if (vnpayItem) vnpayItem.classList.add('active', 'vnpay-active');

        if (currTrigger) currTrigger.classList.remove('disabled');
        if (currHint) currHint.style.display = 'none';

        if (payBtn) payBtn.className = 'quick-pay-btn btn-vnpay';
        if (payText) payText.textContent = 'Thanh toán qua VNPAY';

    } else if (gateway === 'PAYPAL') {
        if (paypalItem) paypalItem.classList.add('active', 'paypal-active');

        // Force USD for PayPal
        setQuickCurrency('USD');
        if (currTrigger) currTrigger.classList.add('disabled');
        if (currHint) {
            currHint.innerHTML = '<i class="fa-solid fa-circle-info"></i> PayPal chỉ hỗ trợ thanh toán bằng Đô la Mỹ (USD)';
            currHint.style.display = 'flex';
        }

        if (payBtn) payBtn.className = 'quick-pay-btn btn-paypal';
        if (payText) payText.textContent = 'Thanh toán qua PayPal';

    } else {
        if (stripeItem) stripeItem.classList.add('active');

        if (currTrigger) currTrigger.classList.remove('disabled');
        if (currHint) currHint.style.display = 'none';

        if (payBtn) payBtn.className = 'quick-pay-btn btn-stripe';
        if (payText) payText.textContent = 'Thanh toán qua Stripe';
    }

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

// Close custom dropdown on outside click
document.addEventListener('click', (e) => {
    const wrapper = document.getElementById('quick-curr-wrapper');
    if (wrapper && !wrapper.contains(e.target)) {
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
    if (overlay) {
        overlay.classList.add('active');
    }

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
            if (overlay) overlay.classList.remove('active');
            if (typeof showToast === 'function') showToast('Không thể tạo đơn hàng. Vui lòng thử lại!');
            return;
        }

        const orderData = await orderRes.json();
        const orderCode = orderData.data.orderCode;

        const paymentRes = await fetch('/api/payments/initiate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                orderCode: orderCode,
                paymentGateway: quickGateway
            })
        });

        if (!paymentRes.ok) {
            const errData = await paymentRes.json().catch(() => null);
            const errMsg = errData?.message || `Không thể kết nối cổng ${quickGateway}!`;
            if (overlay) overlay.classList.remove('active');
            if (typeof showToast === 'function') showToast(errMsg);
            return;
        }

        const paymentData = await paymentRes.json();
        const paymentUrl = paymentData.data.paymentUrl;

        window.location.href = paymentUrl;

    } catch (err) {
        if (overlay) overlay.classList.remove('active');
        if (typeof showToast === 'function') showToast('Lỗi kết nối máy chủ. Vui lòng thử lại!');
    }
}
