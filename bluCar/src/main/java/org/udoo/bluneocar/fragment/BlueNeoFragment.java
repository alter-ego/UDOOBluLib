package org.udoo.bluneocar.fragment;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.udoo.bluneocar.BluNeoCarApplication;
import org.udoo.bluneocar.Util;
import org.udoo.bluneocar.databinding.BlueneocarLayoutBinding;
import org.udoo.bluneocar.manager.BluCarCommandManager;
import org.udoo.bluneocar.viewModel.CommandViewModel;
import org.udoo.udooblulib.interfaces.OnCharacteristicsListener;
import org.udoo.udooblulib.manager.UdooBluManager;
import org.udoo.udooblulib.sensor.UDOOBLESensor;
import org.udoo.udooblulib.utils.Point3D;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Created by harlem88 on 24/03/16.
 */
public class BlueNeoFragment extends Fragment{
    private BlueneocarLayoutBinding mViewBindig;
    private CommandViewModel mCommandViewModel;
    private float mCurrentPitch, mCurrentRoll;
    private String mAddress;
    private UdooBluManager udooBluManager;

    public static BlueNeoFragment Builder(String address){
        BlueNeoFragment fragment = new BlueNeoFragment();
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null)
            mAddress = getArguments().getString("address");

        udooBluManager = ((BluNeoCarApplication) getActivity().getApplication()).getBluManager();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mViewBindig = BlueneocarLayoutBinding.inflate(inflater, container, false);
        mCommandViewModel = new CommandViewModel(new BluCarCommandManager(udooBluManager, mAddress));
        mViewBindig.setCommand(mCommandViewModel);
        return mViewBindig.getRoot();
    }


    @Override
    public void onStart() {
        super.onStart();


        udooBluManager.enableSensor(mAddress, UDOOBLESensor.ACCELEROMETER, true);
        udooBluManager.setNotificationPeriod(mAddress, UDOOBLESensor.ACCELEROMETER);

        udooBluManager.enableSensor(mAddress, UDOOBLESensor.MAGNETOMETER, true);
        udooBluManager.setNotificationPeriod(mAddress, UDOOBLESensor.MAGNETOMETER);

        final Observable<float[]> accelerometerObservable = Observable.create(new Observable.OnSubscribe<float[]>() {
            @Override
            public void call(final Subscriber<? super float[]> subscriber) {
                udooBluManager.enableNotification(mAddress, true, UDOOBLESensor.ACCELEROMETER, new OnCharacteristicsListener() {
                    @Override
                    public void onCharacteristicsRead(String uuidStr, byte[] value, int status) {}

                    @Override
                    public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {
                        Point3D point3D = UDOOBLESensor.ACCELEROMETER.convert(rawValue);
                        if(point3D != null)
                            subscriber.onNext(point3D.toFloatArray());
                    }
                });
            }
        });

        final Observable<float[]> magnetomerterObservable = Observable.create(new Observable.OnSubscribe<float[]>() {
            @Override
            public void call(final Subscriber<? super float[]> subscriber) {
                udooBluManager.enableNotification(mAddress, true, UDOOBLESensor.MAGNETOMETER, new OnCharacteristicsListener() {
                    @Override
                    public void onCharacteristicsRead(String uuidStr, byte[] value, int status) {}

                    @Override
                    public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {
                        Point3D point3D = UDOOBLESensor.MAGNETOMETER.convert(rawValue);
                        if(point3D != null)
                            subscriber.onNext(point3D.toFloatArray());
                    }
                });
            }
        });

        Observable.zip(accelerometerObservable, magnetomerterObservable, new Func2<float[], float[], float[]>() {
            @Override
            public float[] call(float[] values1, float[] values2) {
                float vv[] = Util.GetOrientationValues(values1, values2);

                //azimuth 0
                //pitch   1
                //roll    2
                Log.i("call: ", vv[0] + " " + vv[1] + " " + vv[2]);
                vv[2] -= 170;
                return vv;
            }
        }).onBackpressureBuffer().subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<float[]>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(float[] o) {
                ObjectAnimator animation = ObjectAnimator.ofFloat(mViewBindig.pitch, "rotation", mCurrentPitch, o[1]);
                animation.setDuration(100);
                animation.setInterpolator(new AccelerateDecelerateInterpolator());
                animation.start();
                mCurrentPitch = o[1];

                ObjectAnimator animation2 = ObjectAnimator.ofFloat(mViewBindig.roll, "rotation", mCurrentRoll, o[2]);
                animation2.setDuration(100);
                animation2.setInterpolator(new AccelerateDecelerateInterpolator());
                animation2.start();
                mCurrentRoll = o[2];
            }
        });


    }
}
