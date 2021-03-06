package com.kotlin.order.ui.activity

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.eightbitlab.rxbus.Bus
import com.eightbitlab.rxbus.registerInBus
import com.kotlin.base.ext.onClick
import com.kotlin.base.ext.setVisible
import com.kotlin.base.ui.activity.BaseMvpActivity
import com.kotlin.base.utils.YuanFenConverter
import com.kotlin.order.R
import com.kotlin.order.data.protocol.Order
import com.kotlin.order.data.protocol.ShipAddress
import com.kotlin.order.event.SelectAddressEvent
import com.kotlin.order.injection.component.DaggerOrderComponent
import com.kotlin.order.injection.module.OrderModule
import com.kotlin.order.presenter.OrderConfirmPresenter
import com.kotlin.order.presenter.view.OrderConfirmView
import com.kotlin.order.ui.adapter.OrderGoodsAdapter
import com.kotlin.provider.common.ProviderConstant
import com.kotlin.provider.router.RouterPath
import kotlinx.android.synthetic.main.activity_order_confirm.*
import kotlinx.android.synthetic.main.layout.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

@Route(path = RouterPath.OrderCenter.PATH_ORDER_CONFIRM)
class OrderConfirmActivity : BaseMvpActivity<OrderConfirmPresenter>(), OrderConfirmView {


    @Autowired(name = ProviderConstant.KEY_ORDER_ID)
    @JvmField
    var mOrderId: Int = 0

    private lateinit var mAdapter: OrderGoodsAdapter

    private var mCurrentOrder: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirm)

        initView()
        initObserve()
        loadData()

    }

    override fun injectComponent() {

        DaggerOrderComponent.builder().activityComponent(activityComponent)
                .orderModule(OrderModule()).build().inject(this)
        mPresenter.mView = this

    }

    private fun initView() {
        mShipView.onClick {
            startActivity<ShipAddressActivity>()
        }
        mSelectShipTv.onClick {
            startActivity<ShipAddressActivity>()
        }

        mSubmitOrderBtn.onClick {
            mCurrentOrder?.let {
                mPresenter.submitOrder(it)
            }
        }

        mOrderGoodsRv.layoutManager = LinearLayoutManager(this)
        mAdapter = OrderGoodsAdapter(this)
        mOrderGoodsRv.adapter = mAdapter
    }

    private fun initObserve() {
        Bus.observe<SelectAddressEvent>()
                .subscribe { t: SelectAddressEvent ->
                    run {
                        mCurrentOrder?.let {
                            it.shipAddress = t.address
                        }
                        updateAddressView()
                    }

                }
                .registerInBus(this)
    }


    private fun loadData() {
        toast("$mOrderId")
        mPresenter.getOrderById(mOrderId)
    }


    override fun onGetOrderByIdResult(result: Order) {
        mCurrentOrder = result
        mAdapter.setData(result.orderGoodsList)
        mTotalPriceTv.text = "合计${YuanFenConverter.changeF2YWithUnit(result.totalPrice)}"

        updateAddressView()
    }

    private fun updateAddressView() {
        mCurrentOrder?.let {
            if (it.shipAddress == null) {
                mSelectShipTv.setVisible(true)
                mShipView.setVisible(false)
            } else {
                mSelectShipTv.setVisible(false)
                mShipView.setVisible(true)

                mShipNameTv.text = it.shipAddress!!.shipUserName + " " +
                        it.shipAddress!!.shipUserMobile

                mShipAddressTv.text = it.shipAddress!!.shipAddress
            }
        }
    }

    override fun onSubmitOrderResult(result: Boolean) {
        toast("订单提交成功")
        ARouter.getInstance()
                .build(RouterPath.PaySDK.PATH_PAY)
                .withInt(ProviderConstant.KEY_ORDER_ID, mCurrentOrder!!.id)
                .withLong(ProviderConstant.KEY_ORDER_PRICE, mCurrentOrder!!.totalPrice)
                .navigation()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bus.unregister(this)
    }
}