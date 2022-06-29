// const { ToastContainer, toast } = require('react-toastify');
// import 'react-toastify/dist/ReactToastify.css';

const ws = new WebSocket(`ws://localhost:4334`);

const delay = ms => new Promise(
  resolve => setTimeout(resolve, ms)
);

const Main = () => {
  const [appData, setAppData] = React.useState({});
  const [customPressToAdd, setCustomPressToAdd] = React.useState(0);
  const [toast, setToast] = React.useState(``);
  const [numOfToasts, setNumOfToasts] = React.useState(0);

  // const notify = (toastMsg) => toast(toastMsg);
  
  const generateToast = toast => {
    // notify(toastMsg);
    setNumOfToasts(numOfToasts + 1);
    setToast(toast);
    delay(2000).then(() => {
      if (numOfToasts <= 1) {
        setToast(``);
      }
      setNumOfToasts(numOfToasts - 1);
    })
  }

  ws.onmessage = ev => {
    console.log(`received: ${ev.data}`);
    const data = JSON.parse(ev.data);
    if (`responseTo` in data) generateToast(data.responseTo);
    setAppData(data);
  }

  const changeVal = (ev, setFn) => {
    ev.preventDefault();
    if (ev.target.value !== "") {
      setFn(ev.target.value);
    }
  }

  const addPressure = (pressure, ev) => {
    if (ev) ev.preventDefault();
    ws.send(JSON.stringify({
      type: `addPressure`,
      pressure
    }));
  }

  return (
    <div>
      <form onSubmit={ev => addPressure(customPressToAdd, ev)}>
        <label htmlFor="customPressToAdd">Add:&nbsp;</label>
        <input type="number" id="customPressToAdd" max="1000" min="0" value={customPressToAdd} name="customPressToAdd" onChange={ev => changeVal(ev, setCustomPressToAdd)} />
        <input type="submit" value="+" />
      </form>
      <div style={{padding:"10px"}}></div>
      {
        [5, 10, 25, 50, 100, 250, 500].map((val, i) => (
          <button key={i} onClick={() => addPressure(val)}>+{val}MB</button>
        ))
      }
      <br></br>
      <h2>Statistics</h2>
      {Object.keys(appData).map((key, i) => (
        <div key={i}>
          <p><strong>{`${key}: `}</strong>{`${appData[key]}`}</p>
        </div>
      ))}
      <div style={{padding:"10px"}}></div>
      <h3>{toast}</h3>
    </div>
  );

}

ReactDOM.render(<Main />, document.querySelector(`#root`));
