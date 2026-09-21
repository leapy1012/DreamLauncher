/*
 * Vendored Binder stubs matching DreamQuickGalance AIDL
 * (app/src/main/aidl/gd/app/hiboard/overlay/ILauncherOverlay.aidl).
 * Soong does not compile that AIDL into this module; keep in sync manually.
 */
package gd.app.hiboard.overlay;
/**
 * Remote overlay contract matching OPPO/Heytap ILauncherOverlay (core methods).
 * Launcher binds HiboardOverlayService and drives scroll / lifecycle over Binder.
 * Uses windowAttached2 (API ≥ 3) with LayoutParams inside the Bundle.
 */
public interface ILauncherOverlay extends android.os.IInterface
{
  /** Default implementation for ILauncherOverlay. */
  public static class Default implements gd.app.hiboard.overlay.ILauncherOverlay
  {
    @Override public void startScroll() throws android.os.RemoteException
    {
    }
    @Override public void onScroll(float progress) throws android.os.RemoteException
    {
    }
    @Override public void endScroll() throws android.os.RemoteException
    {
    }
    @Override public void windowDetached(boolean isChangingConfigurations) throws android.os.RemoteException
    {
    }
    @Override public void closeOverlay(int flags) throws android.os.RemoteException
    {
    }
    @Override public void onStart() throws android.os.RemoteException
    {
    }
    @Override public void onPause() throws android.os.RemoteException
    {
    }
    @Override public void onResume() throws android.os.RemoteException
    {
    }
    @Override public void onStop() throws android.os.RemoteException
    {
    }
    @Override public void onDestroy() throws android.os.RemoteException
    {
    }
    @Override public void openOverlay(int flags) throws android.os.RemoteException
    {
    }
    @Override public void windowAttached2(android.os.Bundle bundle, gd.app.hiboard.overlay.ILauncherOverlayCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void endScrollWithVelocity(float velocity) throws android.os.RemoteException
    {
    }
    @Override public boolean hasOverlayContent() throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements gd.app.hiboard.overlay.ILauncherOverlay
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an gd.app.hiboard.overlay.ILauncherOverlay interface,
     * generating a proxy if needed.
     */
    public static gd.app.hiboard.overlay.ILauncherOverlay asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof gd.app.hiboard.overlay.ILauncherOverlay))) {
        return ((gd.app.hiboard.overlay.ILauncherOverlay)iin);
      }
      return new gd.app.hiboard.overlay.ILauncherOverlay.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_startScroll:
        {
          this.startScroll();
          break;
        }
        case TRANSACTION_onScroll:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.onScroll(_arg0);
          break;
        }
        case TRANSACTION_endScroll:
        {
          this.endScroll();
          break;
        }
        case TRANSACTION_windowDetached:
        {
          boolean _arg0;
          _arg0 = (0!=data.readInt());
          this.windowDetached(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_closeOverlay:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.closeOverlay(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onStart:
        {
          this.onStart();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onPause:
        {
          this.onPause();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onResume:
        {
          this.onResume();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onStop:
        {
          this.onStop();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_onDestroy:
        {
          this.onDestroy();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_openOverlay:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.openOverlay(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_windowAttached2:
        {
          android.os.Bundle _arg0;
          _arg0 = _Parcel.readTypedObject(data, android.os.Bundle.CREATOR);
          gd.app.hiboard.overlay.ILauncherOverlayCallback _arg1;
          _arg1 = gd.app.hiboard.overlay.ILauncherOverlayCallback.Stub.asInterface(data.readStrongBinder());
          this.windowAttached2(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_endScrollWithVelocity:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.endScrollWithVelocity(_arg0);
          break;
        }
        case TRANSACTION_hasOverlayContent:
        {
          boolean _result = this.hasOverlayContent();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements gd.app.hiboard.overlay.ILauncherOverlay
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void startScroll() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          mRemote.transact(Stub.TRANSACTION_startScroll, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onScroll(float progress) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(progress);
          mRemote.transact(Stub.TRANSACTION_onScroll, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void endScroll() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          mRemote.transact(Stub.TRANSACTION_endScroll, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void windowDetached(boolean isChangingConfigurations) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(((isChangingConfigurations)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_windowDetached, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void closeOverlay(int flags) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(flags);
          boolean _status = mRemote.transact(Stub.TRANSACTION_closeOverlay, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onStart() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onStart, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onPause() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPause, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onResume() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onResume, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onStop() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onStop, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void onDestroy() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onDestroy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void openOverlay(int flags) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(flags);
          boolean _status = mRemote.transact(Stub.TRANSACTION_openOverlay, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void windowAttached2(android.os.Bundle bundle, gd.app.hiboard.overlay.ILauncherOverlayCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _Parcel.writeTypedObject(_data, bundle, 0);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_windowAttached2, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void endScrollWithVelocity(float velocity) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(velocity);
          mRemote.transact(Stub.TRANSACTION_endScrollWithVelocity, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public boolean hasOverlayContent() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_hasOverlayContent, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_startScroll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onScroll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_endScroll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_windowDetached = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_closeOverlay = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onStart = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_onPause = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_onResume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_onStop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_onDestroy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_openOverlay = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_windowAttached2 = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
    static final int TRANSACTION_endScrollWithVelocity = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
    static final int TRANSACTION_hasOverlayContent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "gd.app.hiboard.overlay.ILauncherOverlay";
  public void startScroll() throws android.os.RemoteException;
  public void onScroll(float progress) throws android.os.RemoteException;
  public void endScroll() throws android.os.RemoteException;
  public void windowDetached(boolean isChangingConfigurations) throws android.os.RemoteException;
  public void closeOverlay(int flags) throws android.os.RemoteException;
  public void onStart() throws android.os.RemoteException;
  public void onPause() throws android.os.RemoteException;
  public void onResume() throws android.os.RemoteException;
  public void onStop() throws android.os.RemoteException;
  public void onDestroy() throws android.os.RemoteException;
  public void openOverlay(int flags) throws android.os.RemoteException;
  public void windowAttached2(android.os.Bundle bundle, gd.app.hiboard.overlay.ILauncherOverlayCallback callback) throws android.os.RemoteException;
  public void endScrollWithVelocity(float velocity) throws android.os.RemoteException;
  public boolean hasOverlayContent() throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
  }
}
